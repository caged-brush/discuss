package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

class ContactItem {
    private String contactUid;
    private String name;
    private String info;

    public ContactItem(String contactUid, String name, String info) {
        this.contactUid = contactUid;
        this.name = name;
        this.info = info;
    }

    public String getContactUid() { return contactUid; }
    public String getName() { return name; }
    public String getInfo() { return info; }
}

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private List<ContactItem> contactList;
    private DBHandler dbHandler;
    private String ownerUid;

    public ContactAdapter(List<ContactItem> contactList, DBHandler dbHandler, String ownerUid) {
        this.contactList = contactList;
        this.dbHandler = dbHandler;
        this.ownerUid = ownerUid;
    }

    // Create single View（load item_contact.xml）
    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    // bind data to single View
    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        ContactItem currentItem = contactList.get(position);

        holder.tvName.setText(currentItem.getName());
        holder.tvInfo.setText(currentItem.getInfo());

        // comfirm popup
        holder.btnRemove.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            new MaterialAlertDialogBuilder(v.getContext())
                    .setTitle("Remove Contact")
                    .setMessage("Are you sure you want to remove " + currentItem.getName() + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        dbHandler.removeContact(ownerUid, currentItem.getContactUid());
                        contactList.remove(currentPos);
                        notifyItemRemoved(currentPos);
                        notifyItemRangeChanged(currentPos, contactList.size() - currentPos);
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        TextView tvName;
        TextView tvInfo;
        ImageButton btnRemove;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivContactAvatar);
            tvName = itemView.findViewById(R.id.tvContactName);
            tvInfo = itemView.findViewById(R.id.tvContactInfo);
            btnRemove = itemView.findViewById(R.id.btnRemoveContact);
        }
    }
}