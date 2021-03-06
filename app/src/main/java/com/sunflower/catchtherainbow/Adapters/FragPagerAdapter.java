package com.sunflower.catchtherainbow.Adapters;

/**
 * Created by Alexandr on 05.02.2017.
 */

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import com.sunflower.catchtherainbow.R;
import com.sunflower.catchtherainbow.Views.AudioChooserFragment;
import com.sunflower.catchtherainbow.Views.FragTabAudioFiles;
import com.sunflower.catchtherainbow.Views.FragTabFolders;

/*
private String fragments[];
private Context context;

public FragPagerAdapter(FragmentManager supportFragmentManager, Context applicationContext, TabLayout tabLayout, AudioChooserFragment audioChooserFragment)
{
    this.context = audioChooserFragment.getContext();
    fragments[0] = this.context.getResources().getString(R.string.audio_files);
    fragments[1] = this.context.getResources().getString(R.string.folders);
}*/

public class FragPagerAdapter extends FragmentPagerAdapter //implements  sdffg
{
    private String fragments [] = {"Audio Files", "Folders"};
    private TabLayout tabLayout;
    private FragTabAudioFiles fragTabAudioFiles;
    private FragTabFolders fragTabFolders;
    private AudioChooserFragment audioChooserFragment;

    public FragPagerAdapter(FragmentManager supportFragmentManager, Context applicationContext, TabLayout tabLayout, AudioChooserFragment audioChooserFragment)
    {
        super(supportFragmentManager);
        this.tabLayout = tabLayout;
        this.audioChooserFragment = audioChooserFragment;
    }

    public FragTabAudioFiles GetFragTabAudioFiles()
    {
        return fragTabAudioFiles;
    }

    public FragTabFolders GetFragTabFolders()
    {
        return fragTabFolders;
    }

    public Fragment GetFragmentByPosition(int position)
    {
        Fragment frag = new Fragment();
        if(position == 0) frag = fragTabAudioFiles;
        if(position == 1) frag = fragTabFolders;
        return frag;
    }

    @Override
    public Fragment getItem(int position)
    {
        switch (position)
        {
            case 0:
                fragTabAudioFiles = new FragTabAudioFiles();
                fragTabAudioFiles.AddLinkAudioChooserFragment(audioChooserFragment);
                return fragTabAudioFiles;
            case 1:
                fragTabFolders = new FragTabFolders();
                fragTabFolders.AddLinkAudioChooserFragment(audioChooserFragment);
                return fragTabFolders;
            default:
                return null;
        }
    }

    @Override
    public void startUpdate(ViewGroup container)
    {
        super.startUpdate(container);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_music_note);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_folder);

    }

    @Override
    public int getCount()
    {
        return fragments.length;
    }

    @Override
    public CharSequence getPageTitle(int position)
    {
        return fragments[position];
    }
}

