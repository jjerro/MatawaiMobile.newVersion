package com.dalehi.matawaimobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;



public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private WebView mWebView;
    SwipeRefreshLayout mySwipeRefreshLayout;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private static final int FILECHOOSER_RESULTCODE = 1;

    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI = null;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;
    private String newURL ="file:///android_asset/www/index.html";

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
                super.onActivityResult ( requestCode, resultCode, data );
                return;
            }
            Uri[] results = null;
            // Check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    // If there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse ( mCameraPhotoPath )};
                    }
                } else {
                    String dataString = data.getDataString ();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse ( dataString )};
                    }
                }
            }
            mFilePathCallback.onReceiveValue ( results );
            mFilePathCallback = null;
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            if (requestCode != FILECHOOSER_RESULTCODE || mUploadMessage == null) {
                super.onActivityResult ( requestCode, resultCode, data );
                return;
            }
            if (requestCode == FILECHOOSER_RESULTCODE) {
                if (null == this.mUploadMessage) {
                    return;
                }
                Uri result = null;
                try {
                    if (resultCode != RESULT_OK) {
                        result = null;
                    } else {
                        // retrieve from the private variable if the intent is null
                        result = data == null ? mCapturedImageURI : data.getData ();
                    }
                } catch (Exception e) {
                    Toast.makeText ( getApplicationContext (), "activity :" + e,
                            Toast.LENGTH_LONG ).show ();
                }
                mUploadMessage.onReceiveValue ( result );
                mUploadMessage = null;
            }
        }
        return;
    }



    @SuppressLint({"SetJavaScriptEnabled", "WrongViewCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate ( savedInstanceState );
        setContentView ( R.layout.activity_main );

        if(Build.VERSION.SDK_INT >=23 &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED || ContextCompat
                .checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA
                    }, 1);
        }

        mySwipeRefreshLayout = (SwipeRefreshLayout)this.findViewById(R.id.swipeContainer);

        Toolbar toolbar = (Toolbar) findViewById ( R.id.toolbar );
        setSupportActionBar ( toolbar );

        DrawerLayout drawer = (DrawerLayout) findViewById ( R.id.drawer_layout );
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle (
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close );
        drawer.addDrawerListener ( toggle );
        toggle.syncState ();

        NavigationView navigationView = (NavigationView) findViewById ( R.id.nav_view );
        navigationView.setNavigationItemSelectedListener ( this );

            mWebView = (WebView) findViewById ( R.id.mWebView );
            WebSettings webSettings = mWebView.getSettings ();
            webSettings.setJavaScriptEnabled ( true );

            mWebView.getSettings ().setRenderPriority ( WebSettings.RenderPriority.HIGH );
            mWebView.getSettings ().setCacheMode ( WebSettings.LOAD_DEFAULT );
            //mWebView.getSettings ().setAppCacheEnabled ( true );
            mWebView.setScrollBarStyle ( View.SCROLLBARS_INSIDE_OVERLAY );
            webSettings.setDomStorageEnabled ( true );
            webSettings.setLayoutAlgorithm ( WebSettings.LayoutAlgorithm.NARROW_COLUMNS );
            webSettings.setUseWideViewPort ( true );
            webSettings.setSaveFormData ( true );
            webSettings.setSavePassword ( false );
            webSettings.setEnableSmoothTransition ( true );
            webSettings.setAllowFileAccess ( true );
            webSettings.setLoadWithOverviewMode ( true );

            mWebView.setWebViewClient ( new Client () );
            mWebView.setWebChromeClient ( new ChromeClient () );

            if (Build.VERSION.SDK_INT >= 21) {
                webSettings.setMixedContentMode ( 0 );
                mWebView.setLayerType ( View.LAYER_TYPE_HARDWARE, null );
            } else if (Build.VERSION.SDK_INT >= 19) {
                mWebView.setLayerType ( View.LAYER_TYPE_HARDWARE, null );
            } else if (Build.VERSION.SDK_INT < 19) {
                mWebView.setLayerType ( View.LAYER_TYPE_SOFTWARE, null );
            }
            mWebView.setWebViewClient ( new Callback () );

        mySwipeRefreshLayout.setOnRefreshListener (
                new SwipeRefreshLayout.OnRefreshListener () {
                    @Override
                    public void onRefresh() {
                        if (CheckNetwork.isInternetAvailable(MainActivity.this)) {
                            mWebView.reload ();
                            refresh ();
                        } else{
                            mWebView.loadUrl (newURL);
                            mySwipeRefreshLayout.setRefreshing(false);
                        }
                    }
                }
        );

        if (CheckNetwork.isInternetAvailable(MainActivity.this)) {
            mWebView.loadUrl ( "http://www.matawai.com" );
        }
        else{
            mWebView.loadUrl (newURL);
            mySwipeRefreshLayout.setRefreshing(false);
        }
    }

    private void refresh() {
        mySwipeRefreshLayout.setRefreshing ( true );
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mySwipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        }).start();
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        @SuppressLint("SimpleDateFormat")
        String timeStamp = new SimpleDateFormat ( "yyyyMMdd_HHmmss" ).format ( new Date () );
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory (
                Environment.DIRECTORY_PICTURES );
        File imageFile = File.createTempFile (
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }

    public class Callback extends WebViewClient{
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl){
            Toast.makeText(getApplicationContext(), "Failed loading app!", Toast.LENGTH_SHORT).show();
        }
    }
    public class ChromeClient extends WebChromeClient {
        // For Android 5.0
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, WebChromeClient.FileChooserParams fileChooserParams) {
            // Double check that we don't have any existing callbacks
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePath;
            Intent takePictureIntent = new Intent( MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    ex.printStackTrace ();
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }
            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("image/*");
            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }
            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
            return true;
        }
        // openFileChooser for Android 3.0+
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
            mUploadMessage = uploadMsg;
            // Create AndroidExampleFolder at sdcard
            File imageStorageDir = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES)
                    , "AndroidExampleFolder");
            if (!imageStorageDir.exists()) {
                // Create AndroidExampleFolder at sdcard
                imageStorageDir.mkdirs();
            }
            // Create camera captured image file path and name
            File file = new File(
                    imageStorageDir + File.separator + "IMG_"
                            + String.valueOf(System.currentTimeMillis())
                            + ".jpg");
            mCapturedImageURI = Uri.fromFile(file);
            // Camera capture image intent
            final Intent captureIntent = new Intent(
                    android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            // Create file chooser intent
            Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
            // Set camera intent to file chooser
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
                    , new Parcelable[] { captureIntent });
            // On select image call onActivityResult method of activity
            startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
        }
        // openFileChooser for Android < 3.0
        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            openFileChooser(uploadMsg, "image/*");
        }
        //openFileChooser for other Android versions
        public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                    String acceptType,
                                    String capture) {
            openFileChooser(uploadMsg, acceptType);
        }
    }
    public class Client extends WebViewClient {
        ProgressDialog progressDialog;
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (CheckNetwork.isInternetAvailable(MainActivity.this)) {
                if ((Uri.parse ( url ).getHost ().equals ( "matawai.com" ))
                        || (Uri.parse ( url ).getHost ().equals ( "www.matawai.com" ))) {
                    return false;
                } else if (url.contains ( "mailto:" )) {
                    try {
                        view.getContext ().startActivity (
                                new Intent ( Intent.ACTION_SENDTO, Uri.parse ( url ) ).setType ( "text/plain" ) );
                        return true;
                    } catch (Exception e) {
                        view.getContext ().startActivity (
                                new Intent ( Intent.ACTION_VIEW, Uri.parse ( url ) ) );
                        return true;
                    }
                } else {
                    Intent intent = new Intent ( Intent.ACTION_VIEW, Uri.parse ( url ) );
                    startActivity ( intent );
                    return true;
                }
            }else {
                view.loadUrl(newURL);
                return true;
            }
        }

        //Show loader on url load
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (progressDialog == null) {
                progressDialog = new ProgressDialog (MainActivity.this);
                progressDialog.setMessage("Loading...");
                progressDialog.show ();
            }
        }
        // Called when all page resources loaded
        public void onPageFinished(WebView view, String url) {
            if (mySwipeRefreshLayout.isRefreshing()) {
                mySwipeRefreshLayout.setRefreshing(false);
            } try {
                // Close progressDialog
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    progressDialog = null;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if(mWebView.canGoBack()){
            if (CheckNetwork.isInternetAvailable(MainActivity.this)) {
                mWebView.goBack ();
            } else{
                mWebView.loadUrl ( newURL );
            }
        }
        else{
            closeApp();
            // super.onBackPressed();
        }
    }

    private void closeApp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Menutup Matawai")
                .setCancelable(false)
                .setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setNegativeButton("Tidak", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater ().inflate ( R.menu.main, menu );

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId ();
            //noinspection SimplifiableIfStatement
            if (id == R.id.action_refresh) {
                if (CheckNetwork.isInternetAvailable(MainActivity.this)) {
                    mWebView.reload();
                    refresh ();
                } else {
                    mWebView.loadUrl ( newURL );
                    mySwipeRefreshLayout.setRefreshing(false);
                }
                return true;
            }
        return super.onOptionsItemSelected ( item );
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId ();
        if (CheckNetwork.isInternetAvailable(MainActivity.this)) {
            if (id == R.id.nav_home) {
                mWebView.loadUrl ( "http://www.matawai.com" );
            } else if (id == R.id.nav_account) {
                mWebView.loadUrl ( "http://www.matawai.com/my-account/edit-account/" );
            } else if (id == R.id.nav_cart) {
                mWebView.loadUrl ( "http://www.matawai.com/cart/" );
            } else if (id == R.id.nav_setting) {
                mWebView.loadUrl ( "http://www.matawai.com/vendor_dashboard/" );
            } else if (id == R.id.cat_accessories) {
                mWebView.loadUrl ( "http://www.matawai.com/kategori-produk/aksesoris/" );
            } else if (id == R.id.cat_atk) {
                mWebView.loadUrl ( "http://www.matawai.com/kategori-produk/atk/" );
            } else if (id == R.id.cat_clothes) {
                mWebView.loadUrl ( "http://www.matawai.com/kategori-produk/pakaian/" );
            } else if (id == R.id.cat_culinary) {
                mWebView.loadUrl ( "http://www.matawai.com/kategori-produk/kuliner/" );
            } else if (id == R.id.cat_gadget) {
                mWebView.loadUrl ( "http://www.matawai.com/kategori-produk/gadget/" );
            } else if (id == R.id.cat_handmade) {
                mWebView.loadUrl ( "http://www.matawai.com/kategori-produk/handmade/" );
            } else if (id == R.id.cat_jasa) {
                mWebView.loadUrl ( "http://www.matawai.com/kategori-produk/jasa/" );
            } else if (id == R.id.cat_property) {
                mWebView.loadUrl ( "http://www.matawai.com/kategori-produk/properti/" );
            } else if (id == R.id.cat_smartphone) {
                mWebView.loadUrl ( "http://www.matawai.com/kategori-produk/smartphone/" );
            } else if (id == R.id.cat_tenunan) {
                mWebView.loadUrl ( "http://www.matawai.com/kategori-produk/tenunan/" );
            } else if (id == R.id.cat_ternak) {
                mWebView.loadUrl ( "http://www.matawai.com/kategori-produk/ternak/" );
            } else if (id == R.id.cat_vehicles) {
                mWebView.loadUrl ( "http://www.matawai.com/kategori-produk/kendaraan/" );
            } else if (id==R.id.nav_close){
               closeApp();
            }
        } else{
            mWebView.loadUrl ( newURL );
        }

        DrawerLayout drawer = (DrawerLayout) findViewById ( R.id.drawer_layout );
        drawer.closeDrawer ( GravityCompat.START );
        return true;
    }

    public void buttonClicked(MenuItem item) {
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.search_product, null);
        final EditText searchField = alertLayout.findViewById(R.id.search_field);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Pencarian");
        // this is set the view from XML inside AlertDialog
        alert.setView(alertLayout);
        // disallow cancel of AlertDialog on click of back button and outside touch
        alert.setCancelable(true);

        alert.setPositiveButton("Cari", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String query = searchField.getText().toString();

                Uri uri = Uri.parse("http://www.matawai.com/?s="+ query +"&post_type=product");
                dialog.dismiss ();
                if (CheckNetwork.isInternetAvailable(MainActivity.this)) {
                mWebView.loadUrl ( String.valueOf ( uri ) );
                } else {
                    mWebView.loadUrl ( newURL );
                    mySwipeRefreshLayout.setRefreshing(false);
                }

            }
        });
        AlertDialog dialog = alert.create();
        dialog.show();
    }



}
