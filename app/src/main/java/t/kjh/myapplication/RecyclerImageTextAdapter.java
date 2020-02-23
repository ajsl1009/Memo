package t.kjh.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.skydoves.powermenu.MenuAnimation;
import com.skydoves.powermenu.OnMenuItemClickListener;
import com.skydoves.powermenu.PowerMenu;
import com.skydoves.powermenu.PowerMenuItem;

import org.w3c.dom.Text;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

public class RecyclerImageTextAdapter extends RecyclerView.Adapter<RecyclerImageTextAdapter.ViewHolder> {
    private Activity activity;
    private List<MemoContent> dataList;
    private Realm realm;

    private final static int REQUEST_FOR_EXISTING_MEMO = 2;

     RecyclerImageTextAdapter(Activity activity, List<MemoContent> dataList){
        this.activity = activity;
        this.dataList = dataList;
    }

    // Create and return viewHolder objects for item view.
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    /*
     * Display data corresponding to position in item view of the viewHolder.
     * External library used
     * https://github.com/bumptech/glide
     * To prevent OOM and easily put images in imageView.
     */
    @Override
    public void onBindViewHolder(RecyclerImageTextAdapter.ViewHolder holder, int position) {
        MemoContent data = dataList.get(position);
        holder.title.setText(data.getTitle());
        holder.desc.setText(data.getContent());

        if(data.getPath().size() != 0) {
            Glide.with(holder.itemView.getContext())
                    .load(data.getPath().get(0))
                    .centerCrop()
                    .into(holder.icon);
        }else{
            holder.icon.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size() ;
    }

    private void removeItemView(int position){
        dataList.remove(position);
        notifyItemRemoved(position);
        notifyItemChanged(position, dataList.size());
    }

    private void removeMemo(String text){
        realm = Realm.getDefaultInstance();
        final RealmResults<MemoContent> results = realm.where(MemoContent.class).equalTo("content", text).findAll();

        realm.executeTransaction(new Realm.Transaction(){
            @Override
            @ParametersAreNonnullByDefault
            public void execute(Realm realm) {
                results.deleteFromRealm(0);
            }
        });
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon ;
        TextView title ;
        TextView desc ;
        Button menu;
        PowerMenu powerMenu;
        private  OnMenuItemClickListener<PowerMenuItem> onMenuItemClickListener;

        ViewHolder(final View itemView) {
            super(itemView) ;

            icon = itemView.findViewById(R.id.icon) ;
            title = itemView.findViewById(R.id.title) ;
            desc = itemView.findViewById(R.id.desc) ;
            menu = itemView.findViewById(R.id.menu);

            /*
             * External library used.
             * https://github.com/skydoves/PowerMenu
             * To custom popupmenu.
             */

            onMenuItemClickListener = new OnMenuItemClickListener<PowerMenuItem>() {
                @Override
                public void onItemClick(int position, PowerMenuItem item) {
                    if(position == 0){
                        removeMemo(dataList.get(getAdapterPosition()).getContent());
                        removeItemView(getAdapterPosition());
                    }
                    powerMenu.dismiss();
                }

            };

            powerMenu = new PowerMenu.Builder(itemView.getContext())
                    .addItem(new PowerMenuItem("삭제하기", false))
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

            menu.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    powerMenu.showAsDropDown(v);
                }
            });

            itemView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), MemoinsertActivity.class);
                    intent.putExtra("key", "fromAdapter");
                    intent.putExtra("id", dataList.get(getAdapterPosition()).getId());
                    intent.putExtra("position", getAdapterPosition());
                    ((Activity)v.getContext()).startActivityForResult(intent, REQUEST_FOR_EXISTING_MEMO);
                }
            });
        }
    }
}