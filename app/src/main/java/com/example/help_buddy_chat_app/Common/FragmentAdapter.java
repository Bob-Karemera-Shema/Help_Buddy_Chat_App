package com.example.help_buddy_chat_app.Common;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.help_buddy_chat_app.ChatList.ChatFragment;
import com.example.help_buddy_chat_app.People.PeopleFragment;
import com.example.help_buddy_chat_app.Requests.RequestsFragment;

public class FragmentAdapter extends FragmentStateAdapter {
    public FragmentAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if(position == 1)
        {
            return new RequestsFragment();
        }
        if(position == 2)
        {
            return new PeopleFragment();
        }
        return new ChatFragment();
    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
