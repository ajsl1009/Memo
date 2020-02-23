package t.kjh.myapplication;
import io.realm.RealmObject;

/*
 * In the Realm3.5 version, it was impossible to store the primitive type variable in the list.
 * When using Realm3.5, the class below is used to store the String.
 */
public class UriPath extends RealmObject {
    public UriPath(){}
    String path;
}
