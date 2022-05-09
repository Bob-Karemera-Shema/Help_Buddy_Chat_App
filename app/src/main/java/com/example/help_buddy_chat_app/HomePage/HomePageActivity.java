package com.example.help_buddy_chat_app.HomePage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.example.help_buddy_chat_app.ChatList.ChatFragment;
import com.example.help_buddy_chat_app.People.PeopleFragment;
import com.example.help_buddy_chat_app.Profile.ProfileActivity;
import com.example.help_buddy_chat_app.R;
import com.example.help_buddy_chat_app.Requests.RequestsFragment;
import com.google.android.material.tabs.TabLayout;

public class HomePageActivity extends AppCompatActivity {

    //Declare UI components
    private TabLayout homeTab;
    private ViewPager2 vpHome;
    private boolean leaveApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        leaveApp = false;
        homeTab = findViewById(R.id.homeTab);
        vpHome = findViewById(R.id.vpHome);

        initialiseViewPager();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Called on activity start up to create menus

        getMenuInflater().inflate(R.menu.menu_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == R.id.menu_Profile1)
        {
            //redirect user to profile screen
            startActivity(new Intent(HomePageActivity.this, ProfileActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        //Handler for back button presses
        if(homeTab.getSelectedTabPosition() > 0)
        {
            homeTab.selectTab(homeTab.getTabAt(0));
        }
        else
        {
            finishAffinity();
        }
    }

    private void initialiseViewPager()
    {
        //Declare FragmentManager object to manage fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        //Declare FragmentAdapter object to manage fragments when accessed by the user
        FragmentAdapter fragmentAdapter = new FragmentAdapter(fragmentManager, getLifecycle());
        vpHome.setAdapter(fragmentAdapter);

        //Add tabs to the tab layout
        homeTab.addTab(homeTab.newTab().setCustomView(R.layout.tab_home_chat));
        homeTab.addTab(homeTab.newTab().setCustomView(R.layout.tab_home_requests));
        homeTab.addTab(homeTab.newTab().setCustomView(R.layout.tab_home_people));
        homeTab.setTabGravity(TabLayout.GRAVITY_FILL);

        //add selected tab listeners to update screen
        homeTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                vpHome.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        vpHome.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                homeTab.selectTab(homeTab.getTabAt(position));
            }
        });
    }

    //Inner class to help load fragments
    class FragmentAdapter extends FragmentStateAdapter {
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
            return homeTab.getTabCount();
        }
    }
}