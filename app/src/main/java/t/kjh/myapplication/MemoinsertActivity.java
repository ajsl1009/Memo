package t.kjh.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.snackbar.Snackbar;
import com.sangcomz.fishbun.FishBun;
import com.sangcomz.fishbun.adapter.image.impl.GlideAdapter;
import com.sangcomz.fishbun.define.Define;
import com.skydoves.powermenu.MenuAnimation;
import com.skydoves.powermenu.OnMenuItemClickListener;
import com.skydoves.powermenu.PowerMenu;
import com.skydoves.powermenu.PowerMenuItem;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.ViewPager;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.RealmResults;
import me.relex.circleindicator.CircleIndicator;

/*
 * This is an activity that opens when you register or edit a new memo in Main.
 */
public class MemoinsertActivity extends Activity {
    private static final int PICK_FROM_PHOTO = 2;
    private String mCurrentPhotoPath;
    private TextView textView;
    private Button galleryButton;
    private ViewPager viewPager;
    private ConstraintLayout viewPagerParent;
    private EditText editText_content;
    private EditText editText_title;
    private Button doneButton;
    private CircleIndicator indicator;
    private Button trashButton;

    private String title = "";
    private String content = "";

    private ArrayList<Uri> path;
    private ArrayList<Uri> arrayList = null;

    private int position;
    private long id;
    private Realm realm;

    private PowerMenu powerMenu;
    private OnMenuItemClickListener<PowerMenuItem> onMenuItemClickListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.memo_insert);

        textView = findViewById(R.id.datetext);
        editText_title = findViewById(R.id.titletext);
        editText_content = findViewById(R.id.edittext);
        doneButton = findViewById(R.id.addFinish);
        galleryButton = findViewById(R.id.gallerybutton);
        viewPager = findViewById(R.id.view_pager);
        indicator = findViewById(R.id.indicator);
        viewPagerParent = findViewById(R.id.view_pager_parent);
        trashButton = findViewById(R.id.trash_button);

        /*
         * External library used
         * https://github.com/skydoves/PowerMenu
         */
        onMenuItemClickListener = new OnMenuItemClickListener<PowerMenuItem>() {
            @Override
            public void onItemClick(int position, PowerMenuItem item) {
                // 0 is image from camera
                if(position == 0){
                    int permissionCheck = ContextCompat.checkSelfPermission(MemoinsertActivity.this, Manifest.permission.CAMERA);
                    if(permissionCheck == PackageManager.PERMISSION_DENIED){
                        ActivityCompat.requestPermissions(MemoinsertActivity.this, new String[]{Manifest.permission.CAMERA}, 0);

                    }else {
                        /*Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivityForResult(intent, PICK_FROM_PHOTO);
                        }*/
                        dispatchTakePictureIntent();
                    }
                }
                // 1 is image from album
                else if(position == 1){
                    doTakeMultiAlbum();
                }
                //2 is image from URL
                else{
                    show();
                }

                powerMenu.dismiss();
            }

        };

        //custom powerMenu(popupmenu)
        powerMenu = new PowerMenu.Builder(getBaseContext())
                .addItem(new PowerMenuItem("카메라", false))
                .addItem(new PowerMenuItem("갤러리", false))
                .addItem(new PowerMenuItem("URL등록", false))
                .setAnimation(MenuAnimation.SHOWUP_TOP_LEFT) // Animation start point (TOP | LEFT).
                .setMenuRadius(10f) // sets the corner radius.
                .setMenuShadow(10f) // sets the shadow.
                .setTextColor(Color.BLACK)
                .setTextGravity(Gravity.CENTER)
                .setTextTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
                .setSelectedTextColor(Color.WHITE)
                .setMenuColor(Color.WHITE)
                .setOnMenuItemClickListener(onMenuItemClickListener)
                .build();


        Intent intent = getIntent();
        String key = intent.getStringExtra("key");

        /*
         * Distinguish new memo from editing memo
         * fromFAB means request for new memo
         */
        if(key.equals("fromFAB")) {
            Date currentTime = Calendar.getInstance().getTime();
            String date_text = new SimpleDateFormat("yyyy년 MM월 dd일 EE요일", Locale.getDefault()).format(currentTime);
            textView.setText(date_text);
            viewPagerParent.setVisibility(View.GONE);
            doneButton.setOnClickListener(new btnClickListener());
            galleryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    powerMenu.showAsDropDown(v);
                }
            });
            trashButton.setOnClickListener(new TrashButtonClickListener());
        }
        //request for editing memo
        else{
            Date currentTime = Calendar.getInstance().getTime();
            String date_text = new SimpleDateFormat("yyyy년 MM월 dd일 EE요일", Locale.getDefault()).format(currentTime);
            textView.setText(date_text);
            if(intent.getExtras() != null) {
                id = intent.getExtras().getLong("id");
                position = intent.getExtras().getInt("position");
            }else{
                Log.d("Intent", "something's wrong");
            }


            // Find object in DB using id(primary key) field
            realm = Realm.getDefaultInstance();
            final MemoContent memoContent = realm.where(MemoContent.class)
                    .equalTo("id", id).findFirst();

            editText_title.setText(memoContent.getTitle());
            editText_content.setText(memoContent.getContent());

            RealmList<String> realmList;
            realmList = memoContent.getPath();
            arrayList = new ArrayList<>();

            //Casting all strings to URI
            for(String str : realmList){
                arrayList.add(Uri.parse(str));
            }

            if(arrayList.size() == 0){          /*None image*/
                viewPagerParent.setVisibility(View.GONE);
            }else {             /* There exists some image Uri in arrayList*/
                setAdapterAndIndicator(this, arrayList);
            }

            //To remove image when clicking trash button
            trashButton.setOnClickListener(new TrashButtonClickListener());


            doneButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    title = editText_title.getText().toString();
                    content = editText_content.getText().toString();
                    Intent intent = new Intent();
                    intent.putExtra("title", title);
                    intent.putExtra("content", content);
                    intent.putExtra("id", id);
                    intent.putExtra("position", position);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("thumbNail", arrayList);
                    intent.putExtras(bundle);

                    setResult(RESULT_OK, intent);
                    finish();
                }
            });


            galleryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    powerMenu.showAsDropDown(v);
                }
            });
        }
    }

    private void setAdapterAndIndicator(Context context, ArrayList<Uri> arrayList){
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(context, arrayList);
        viewPager.setAdapter(viewPagerAdapter);
        indicator.setViewPager(viewPager);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "t.kjh.myapplication.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, PICK_FROM_PHOTO);
            }
        }
    }


    //To show dialog for URL
    private void show()
    {
        final EditText edittext = new EditText(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("URL 이미지 등록");
        builder.setMessage("URL을 입력하세요");
        builder.setView(edittext);
        builder.setPositiveButton("입력",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String url = edittext.getText().toString();
                        if(isUrlValid(url)){            /*Check URL form*/
                            CheckFileExist checkFileExist = new CheckFileExist();
                            checkFileExist.execute(url);
                        }else{          /*Url form is not valid*/
                            Toast.makeText(getApplicationContext(),"URL 형식이 잘못되었습니다." ,Toast.LENGTH_LONG).show();
                        }
                    }
                });
        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.show();
    }

    /*Check URL form is valid*/
    private boolean isUrlValid(String urlString){
        try {
            URL url = new URL(urlString);
            return URLUtil.isValidUrl(urlString) && Patterns.WEB_URL.matcher(urlString).matches();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
    }



    /*
     * When you press the Back button,
     * if you want the Memo you've written to be stored in the DB,
     * remove the comment surrounding onBackPressed() method below.
     */

    /*
    @Override
    public void onBackPressed() {
        title = editText_title.getText().toString();
        content = editText_content.getText().toString();
        Intent add = new Intent();
        add.putExtra("title", title);
        add.putExtra("content", content);
        add.putExtra("id", id);
        add.putExtra("position", position);
        Bundle bundle = new Bundle();
        bundle.putSerializable("thumbNail", arrayList);
        add.putExtras(bundle);

        setResult(RESULT_OK, add);
        finish();
        super.onBackPressed();
    }*/

    class btnClickListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            title = editText_title.getText().toString();
            content = editText_content.getText().toString();
            Intent add = new Intent();
            add.putExtra("title", title);
            add.putExtra("content", content);
            Bundle bundle = new Bundle();
            bundle.putSerializable("thumbNail", arrayList);
            add.putExtras(bundle);

            setResult(RESULT_OK, add);
            finish();
        }
    }
    class TrashButtonClickListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            if(arrayList.size() > 0){
                arrayList.remove(viewPager.getCurrentItem());
                setAdapterAndIndicator(viewPager.getContext(), arrayList);
                if(arrayList.size() == 0){
                    viewPagerParent.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        /*if(requestCode == 0){
            if(grantResults[0] == 0){
            }
        }*/
    }

    /*
     * External library used
     * https://github.com/sangcomz/FishBun
     * Libraries for selecting multiple pictures in the gallery
     */
    private void doTakeMultiAlbum(){
        FishBun.with(MemoinsertActivity.this)
                .setImageAdapter(new GlideAdapter())
                .setCamera(true)
                .startAlbum();
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageData) {
        super.onActivityResult(requestCode, resultCode, imageData);
        switch (requestCode) {
            case Define.ALBUM_REQUEST_CODE:
                // Selecting multiple images in gallery did well
                if (resultCode == RESULT_OK) {
                    path = imageData.getParcelableArrayListExtra(Define.INTENT_PATH);
                    //path.toArray().toString();

                    viewPager = findViewById(R.id.view_pager);

                    if(viewPagerParent.getVisibility() == View.GONE){
                        viewPagerParent.setVisibility(View.VISIBLE);
                    }

                    if(arrayList != null){          /*Images already in the arrayList*/
                        arrayList.addAll(path);
                        setAdapterAndIndicator(this, arrayList);
                    }else {             /*There is no image in the arrayList*/
                        arrayList = path;
                        setAdapterAndIndicator(this, arrayList);
                    }

                    break;
                }
            case PICK_FROM_PHOTO :
                if(resultCode == RESULT_OK){
                    //Uri photoUri = imageData.getData();
                    File file = new File(mCurrentPhotoPath);
                    Uri photoUri = Uri.fromFile(file);

                    if(viewPagerParent.getVisibility() == View.GONE){
                        viewPagerParent.setVisibility(View.VISIBLE);
                    }

                    if(arrayList != null){
                        arrayList.add(photoUri);
                        setAdapterAndIndicator(this, arrayList);
                    }else {
                        arrayList = new ArrayList<>();
                        arrayList.add(photoUri);
                        Log.d("arrayList", arrayList.get(0).toString());
                        setAdapterAndIndicator(this, arrayList);
                    }

                    break;
                }
        }
    }

/*
 * AsyncTask to verify that the image is actually a URL.
 * @param params : URL that we want to check
 */
    private class CheckFileExist extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {}

        @Override
        protected String doInBackground(String... params) {

            try {
                HttpURLConnection.setFollowRedirects(false);
                HttpURLConnection con =  (HttpURLConnection) new URL(params[0]).openConnection();
                con.setRequestMethod("HEAD");
                if(con.getResponseCode() == HttpURLConnection.HTTP_OK){
                    return params[0];
                }else{
                    return null;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null)
            {
                arrayList.add(Uri.parse(result));
                setAdapterAndIndicator(viewPager.getContext(), arrayList);
            }
            else
            {
                Toast.makeText(MemoinsertActivity.this, "이미지가 존재하지않습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
