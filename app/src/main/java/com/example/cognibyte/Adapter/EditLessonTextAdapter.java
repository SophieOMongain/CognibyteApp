package com.example.cognibyte.Adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cognibyte.R;

import java.util.List;

public class EditLessonTextAdapter extends RecyclerView.Adapter<EditLessonTextAdapter.ViewHolder> {
    private List<String> items;
    public EditLessonTextAdapter(List<String> items) {
        this.items = items;
    }
    public List<String> getItems(){
        return items;
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public EditText editText;
        public ViewHolder(View view) {
            super(view);
            editText = view.findViewById(R.id.editableText);
        }
    }
    @NonNull
    @Override
    public EditLessonTextAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.edit_item, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull EditLessonTextAdapter.ViewHolder holder, int position) {
        holder.editText.setText(items.get(position));
        holder.editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){}
            @Override public void afterTextChanged(Editable s) {
                items.set(position, s.toString());
            }
        });
    }
    @Override
    public int getItemCount() {
        return items.size();
    }
}
