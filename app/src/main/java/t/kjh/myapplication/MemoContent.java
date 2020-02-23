package t.kjh.myapplication;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/*
 * RealmObject schema
 * Unlike sqlite, Realm is stored in db as an object.
 * Realm 4.0.0
 * https://realm.io/kr
 */
public class MemoContent extends RealmObject {

    @PrimaryKey
    private long id;    /*use "long" instead of "int" in Realm*/

    private String title;
    private String content;
    private RealmList<String> path = new RealmList<>();

    /*@Override
    public String toString() {
        return "Memo{" +
                "text='" + content + '\'' +
                '}';
    }*/

    public MemoContent(){
    }

     MemoContent(long id, String title, String content,  RealmList<String> path){
        this.id = id;
        this.title = title;
        this.content = content;
        this.path = path;
    }

     void setContent(String title, String content,  RealmList<String> path){
        this.content = content;
        this.title = title;
        this.path = path;
    }

     long getId(){return id;}

     String getTitle(){return title;}

     String getContent(){return content;}

     RealmList<String> getPath(){return path;}

}
