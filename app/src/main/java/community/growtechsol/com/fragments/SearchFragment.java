package community.growtechsol.com.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import community.growtechsol.com.adapters.UserAdapter;
import community.growtechsol.com.databinding.FragmentSearchBinding;
import community.growtechsol.com.models.User;

public class SearchFragment extends Fragment {

    FragmentSearchBinding binding;
    ArrayList<User> list = new ArrayList<User>();
    FirebaseAuth auth;
    FirebaseDatabase firebaseDatabase;
    UserAdapter userAdapter;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentSearchBinding.inflate(inflater, container, false);

        userAdapter = new UserAdapter(getContext(), list);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        binding.usersRv.setLayoutManager(linearLayoutManager);
        binding.usersRv.setAdapter(userAdapter);

        firebaseDatabase.getReference().child("Users").addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    user.setUserId(dataSnapshot.getKey());
                    if (!dataSnapshot.getKey().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                        list.add(user);
                    }

                }
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return true;
            }
        });


        return binding.getRoot();
    }

    private void filterList(String newText) {
        ArrayList<User> filteredList = new ArrayList<>();

        for (User user : list) {
            if (user.getName().toLowerCase().contains(newText.toLowerCase())) {
                filteredList.add(user);
            }
        }

        if (filteredList.isEmpty()) {
            Toast.makeText(getContext(), "Not Data Found!", Toast.LENGTH_SHORT).show();
        } else {
            userAdapter.setFilteredList(filteredList);
        }
    }
}