package com.example.myapp.HomeFragments;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class HomePagerAdapter extends FragmentStateAdapter {

    public HomePagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new CommunityFragment();
            case 1:
                return new RankFragment();
            case 2:
                return new RouteFragment();
            default:
                return new CommunityFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
    @Override
    public long getItemId(int position) {
        switch (position) {
            case 0:
                return "CommunityFragment".hashCode();
            case 1:
                return "RankFragment".hashCode();
            case 2:
                return "RouteFragment".hashCode();
            default:
                return position;
        }
    }

    @Override
    public boolean containsItem(long itemId) {
        return itemId == "CommunityFragment".hashCode() ||
                itemId == "RankFragment".hashCode() ||
                itemId == "RouteFragment".hashCode();
    }



}
