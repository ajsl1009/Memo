/*
 * 2020/02/22
 * copyright (c) 2020 JaeHwan Kim
 * Programmers, Line, ANDROID APP assignment
 * All rights reserved
 */
package t.kjh.myapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmResults;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_FOR_NEW_MEMO = 1;

    private RecyclerView recyclerView = null ;
    private RecyclerImageTextAdapter recyclerImageTextAdapter = null ;
    private MemoContent memoContent;
    private List<MemoContent> list = new ArrayList<>();
    private Realm realm; /*Database object*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new FABClickListener());

        recyclerView = findViewById(R.id.memoList) ;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, linearLayoutManager.getOrientation()));
        recyclerView.setLayoutManager(linearLayoutManager);

        /*
         * Library address : https://realm.io/kr
         * Using realm DB for local storage
         */
        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();           /*Create real file*/
        //Save the above set options to the real object
        Realm.setDefaultConfiguration(config);
        realm = Realm.getDefaultInstance();
        //Received by requesting a complete MemoContent list in realm
        RealmResults<MemoContent> realmResults = realm.where(MemoContent.class).findAllAsync();

        //The list of all memoContent in the real file is displayed on each item in the recyclerView.
        for(MemoContent memoContent : realmResults){
            list.add(new MemoContent(memoContent.getId(), memoContent.getTitle(), memoContent.getContent(),memoContent.getPath()));
            recyclerImageTextAdapter = new RecyclerImageTextAdapter(MainActivity.this, list);
            recyclerView.setAdapter(recyclerImageTextAdapter);
        }
    }


    /*
     *The reason for adding the key value "fromFAB" is
     *to distinguish new registration from editing.
     */
    class FABClickListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), MemoinsertActivity.class);
            intent.putExtra("key", "fromFAB");
            startActivityForResult(intent, REQUEST_FOR_NEW_MEMO);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            /*Executes when a new Memo is registered.*/
            if (requestCode == REQUEST_FOR_NEW_MEMO) {
                String title;
                String content;
                ArrayList<Uri> Uri_list;
                if(data != null && data.getExtras() != null) {
                    title = data.getExtras().getString("title");
                    content = data.getExtras().getString("content");
                    Bundle bundle = data.getExtras();
                    Uri_list = (ArrayList<Uri>) bundle.getSerializable("thumbNail");
                }
                else{
                    title = "";
                    content = "";
                    Uri_list = null;
                }
                RealmList<String> realmList = new RealmList<>();

                /*
                 * Convert the value passed to ArrayList <Uri> to String and save it to RealmList.
                 * Because RealmList Object only can save primitive type in Realm database.
                 */
                if (Uri_list != null) {
                    for (Uri uri : Uri_list) {
                        realmList.add(uri.toString());
                    }
                }

                /*
                 * Add new memo in DB.
                 * There is no Auto-increment primary key in Realm.
                 */
                realm.beginTransaction();
                Number maxValue = realm.where(MemoContent.class).max("id");
                long pk = (maxValue != null) ? maxValue.intValue() + 1 : 0; /*pk means primary key*/
                memoContent = realm.createObject(MemoContent.class, pk);
                memoContent.setContent(title, content, realmList);
                realm.commitTransaction();


                //Add a new MemoContent object to the list and register an adapter
                list.add(new MemoContent(pk, title, content, realmList));
                recyclerImageTextAdapter = new RecyclerImageTextAdapter(MainActivity.this, list);
                recyclerView.setAdapter(recyclerImageTextAdapter);
            }

            /*Executes when editing a memo that existed*/
            else{
                final String title = data.getExtras().getString("title");
                final String content = data.getExtras().getString("content");
                final long id = data.getExtras().getLong("id");          /*To select object in Realm DB*/
                final int position = data.getExtras().getInt("position");           /*To select item in recyclerView*/
                Bundle bundle = data.getExtras();
                ArrayList<Uri> Uri_list = (ArrayList<Uri>) bundle.getSerializable("thumbNail");
                final RealmList<String> realmList = new RealmList<>();


                // A reason to convert is same as above.
                if (Uri_list != null) {
                    for (Uri uri : Uri_list) {
                        realmList.add(uri.toString());
                    }
                }

                //Update MemoContent Transaction in realm DB
                realm.executeTransaction(new Realm.Transaction(){
                    @Override
                    @ParametersAreNonnullByDefault
                    public void execute(Realm realm) {
                        MemoContent memoContent = realm.where(MemoContent.class)
                                .equalTo("id", id).findFirst();
                        if(memoContent != null) {
                            memoContent.setContent(title, content, realmList);
                        }
                    }
                });


                list.set(position, new MemoContent(id, title, content, realmList));
                recyclerImageTextAdapter = new RecyclerImageTextAdapter(MainActivity.this, list);
                recyclerView.setAdapter(recyclerImageTextAdapter);
            }
        }
    }
}
