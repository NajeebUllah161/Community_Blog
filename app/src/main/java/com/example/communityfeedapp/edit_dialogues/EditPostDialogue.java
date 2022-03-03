package com.example.communityfeedapp.edit_dialogues;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.example.communityfeedapp.databinding.ActivityEditPostDialogueBinding;

public class EditPostDialogue extends Dialog implements View.OnClickListener {

    ActivityEditPostDialogueBinding binding;

    public EditPostDialogue(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditPostDialogueBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    public void onClick(View view) {
        //Handle click
    }
}