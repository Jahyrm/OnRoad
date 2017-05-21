package com.wwecuador.onroad.logic;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.wwecuador.onroad.AlertasCercanasFragment;
import com.wwecuador.onroad.AllAlertsFragment;
import com.wwecuador.onroad.MisAlertasFragment;
import com.wwecuador.onroad.R;

/**
 * Created by Jahyr on 19/5/2017.
 */

public class CategoryAdapter extends FragmentPagerAdapter {

    Context mContext;

    public CategoryAdapter(FragmentManager fm, Context context) {
        super(fm); mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        switch(position) {
            case 0:
                return new AllAlertsFragment();
            case 1:
                return new AlertasCercanasFragment();
            default:
                return new MisAlertasFragment();
        }
    }

    @Override
    public CharSequence getPageTitle(int position){
        switch(position) {
            case 0:
                return mContext.getResources().getString(R.string.all_alerts);
            case 1:
                return mContext.getResources().getString(R.string.near_alerts);
            default:
                return mContext.getResources().getString(R.string.mine_alerts);
        }
    }

    @Override
    public int getCount() {
        return 3;
    }
}
