package com.sunflower.catchtherainbow;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sunflower.catchtherainbow.Adapters.PopupSeekbarAdapter;
import com.sunflower.catchtherainbow.Adapters.SupportedLanguages;
import com.sunflower.catchtherainbow.AudioClasses.AudioExporter;
import com.sunflower.catchtherainbow.AudioClasses.AudioFile;
import com.sunflower.catchtherainbow.AudioClasses.AudioIO;
import com.sunflower.catchtherainbow.AudioClasses.AudioImporter;
import com.sunflower.catchtherainbow.AudioClasses.BasePlayer;
import com.sunflower.catchtherainbow.AudioClasses.Clip;
import com.sunflower.catchtherainbow.AudioClasses.Project;
import com.sunflower.catchtherainbow.AudioClasses.WaveTrack;
import com.sunflower.catchtherainbow.Views.AudioChooserFragment;
import com.sunflower.catchtherainbow.Views.AudioChooserFragment.OnFragmentInteractionListener;
import com.sunflower.catchtherainbow.Views.AudioProgressView;
import com.sunflower.catchtherainbow.Views.AudioVisualizerView;
import com.sunflower.catchtherainbow.Views.Editing.MainAreaFragment;
import com.sunflower.catchtherainbow.Views.Editing.WaveTrackView;
import com.sunflower.catchtherainbow.Views.Effects.EffectsHostFragment;
import com.sunflower.catchtherainbow.Views.Helpful.ExportSongFragment;
import com.sunflower.catchtherainbow.Views.Helpful.LanguageFragment;
import com.sunflower.catchtherainbow.Views.Helpful.SuperSeekBar;
import com.sunflower.catchtherainbow.Views.StartedApp.ProjectStartActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Queue;

public class ProjectActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnFragmentInteractionListener,
        View.OnClickListener, EffectsHostFragment.OnEffectsHostListener,
        ExportSongFragment.OnFragmentExportSongListener
{
    private static final String TAG = "Project";

    private int notificationId = 0;

    private ActionMenuView amvMenu;
    private ImageButton playPauseButt, bNext, bPrev, bRecorderStart, bStop, bSettings;
    private TextView zoomText, selectionText;
    private AudioProgressView progressView;
    private View viewContentProject;
    private AudioVisualizerView visualizerView;
    private NavigationView navigationView;

    private RelativeLayout waveFormViewContainer;
    private MainAreaFragment tracksFragment;

    // updates status(position and time)
    private Handler statusHandler = new Handler();
    // handles visual updates
    Runnable updateTimer;

    private boolean isDragging = false;
    private AudioIO player;

    // opened project (mustn't be null)
    private Project project;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // ----------Language-----------
        SharedPreferences shared = getSharedPreferences("info", MODE_PRIVATE);
        String language = "English";
        if(shared != null) language = shared.getString("language", "English");
        currentLanguage = SupportedLanguages.valueOf(language);
        // ----------Language end-----------

        setContentView(R.layout.activity_project);

        ////////////////////////////////////////////////////permissions//////////////////////////
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT)
        {
            int PERMISSION_ALL = 1;
            String[] PERMISSIONS = new String[0];

            PERMISSIONS = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAPTURE_AUDIO_OUTPUT,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE};

            if (!hasPermissions(this, PERMISSIONS))
            {
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            }
        }
        //////////////////////////////////////////////////////////end permissions///////////////////////

        viewContentProject = (View) findViewById(R.id.content_project);

        // Init toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        amvMenu = (ActionMenuView) findViewById(R.id.amvMenu);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Init drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Init nav view
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // -----------------------------------------------------FOR TESTING PURPOSES---------------------------------------
        visualizerView = (AudioVisualizerView) findViewById(R.id.audioVisualizerView);
        //visualizerView.setDrawingKind(AudioVisualizerView.DrawingKind.Points);

        waveFormViewContainer = (RelativeLayout) findViewById(R.id.mainAreaContainer);

        // play/stop/next/prev
        playPauseButt = (ImageButton) findViewById(R.id.Sacha);
        bNext = (ImageButton) findViewById(R.id.playNext);
        bPrev = (ImageButton) findViewById(R.id.playPrev);
        bRecorderStart = (ImageButton) findViewById(R.id.bRecorderStart);
        bStop = (ImageButton) findViewById(R.id.bStop);
        bSettings = (ImageButton) findViewById(R.id.settings);

        // play stop handler
        playPauseButt.setOnClickListener(this);

        bPrev.setOnClickListener(this);
        bNext.setOnClickListener(this);
        bRecorderStart.setOnClickListener(this);
        bStop.setOnClickListener(this);
        bSettings.setOnClickListener(this);

        // additional info
        zoomText = (TextView)findViewById(R.id.zoomText);
        selectionText = (TextView)findViewById(R.id.selectionText);

        progressView = (AudioProgressView) findViewById(R.id.audioProgressView);
        progressView.setMax(1.f);
        progressView.setCurrent(0);
        progressView.setOnSeekBar(new SuperSeekBar.OnSuperSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SuperSeekBar seekBar, float progress, boolean fromUser)
            {
                // updates text
                progressView.setCurrent(progress);

                WaveTrack longestTrack = tracksFragment.findLongestTrack();

                if(longestTrack == null) return;
                long samples = longestTrack.timeToSamples(progress);

                tracksFragment.setOffset(samples);
            }
            @Override
            public void onStartTrackingTouch(SuperSeekBar seekBar)
            {
                isDragging = true;
            }

            @Override
            public void onStopTrackingTouch(SuperSeekBar seekBar)
            {
                if (player != null)
                {
                    player.setPosition(seekBar.getCurrentValue());
                }
                isDragging = false;
            }
        });

        // -----------------------------------------------
        Intent intent = getIntent();
        String nameProject = intent.getStringExtra("nameProject");
        String openProjectWithName = intent.getStringExtra("openProjectWithName");

        if(nameProject != null) project = Project.createNewProject(nameProject, projectListener);
        if(openProjectWithName != null)
        {
            Project.openProjectAsync(openProjectWithName, projectListener);
        }
        // ----------------------------- finish handling project----------------------------
    }

    @Override
    protected void onStop()
    {
        if(player != null)
            player.stop();
        if(statusHandler != null)
            statusHandler.removeCallbacks(updateTimer);

        if(tracksFragment != null)
            tracksFragment.stopDrawing();

        if(project != null)
            project.updateProjectFile();

        super.onStop();
    }

    @Override
    public void onPause()
    {
        if(tracksFragment != null)
            tracksFragment.stopDrawing();

        if(project != null)
            project.updateProjectFile();

        super.onPause();
    }

    @Override
    public void onResume()
    {
        if(tracksFragment != null)
            tracksFragment.startDrawing();

        super.onResume();
    }

    @Override
    public void onDestroy()
    {
        if(player != null)
            player.eject();
        statusHandler.removeCallbacks(updateTimer);

        // stop rendering threads
        if(tracksFragment != null)
            tracksFragment.stopDrawing();

        if(project != null)
        {
            project.removeListener(projectListener);
            project.updateProjectFile();
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // remove the notification in case it's there
        notificationManager.cancel(notificationId);

        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
        }
        else
        {
            //super.onBackPressed();
        }
    }

    MenuItem action_effects, action_move, action_select, action_editing;
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.project, /*amvMenu.getMenu() /**/menu); // adds menu items to the left side. If it's not needed replace second param with default menu
        action_effects = menu.findItem(R.id.action_effects);
        action_move = menu.findItem(R.id.action_move);
        action_select = menu.findItem(R.id.action_select);
        action_editing = menu.findItem(R.id.action_editing);
        //menu.findItem(R.id.action_effects).setEnabled(false);
        return true;
    }

    private WaveTrack bufferedTrack = null;

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id)
        {
            case R.id.action_move:
                tracksFragment.setMode(MainAreaFragment.Mode.Hand);
                break;

            case R.id.action_select:
                tracksFragment.setMode(MainAreaFragment.Mode.Selection);
                break;

            case R.id.action_remove: case R.id.action_copy: case R.id.action_paste: case R.id.action_effects:
            {
                WaveTrack selectedTrack = tracksFragment.getSelectedTrack();

                if(selectedTrack == null)
                {
                    Helper.showCuteToast(ProjectActivity.this, R.string.track_not_selected, Gravity.CENTER, Toast.LENGTH_SHORT); // error
                    return super.onOptionsItemSelected(item);
                }

                // stereo!!!
                double start = tracksFragment.getSelectionStartTime()*2.0;
                double end = tracksFragment.getSelectionEndTime()*2.0;

                //Helper.showCuteToast(ProjectActivity.this, "S : " + Helper.round(start, 2) + ", E: " + Helper.round(end, 2), Gravity.CENTER);
                //Log.e("Area! ", "Start : " + start + ", End: " + end);

                if (id == R.id.action_remove)
                {
                    try
                    {
                        if(selectedTrack.clear(start, end))
                        {
                            player.initialize(false);
                            tracksFragment.setSelection(new MainAreaFragment.SampleRange(0,1));
                            tracksFragment.demandUpdate();
                        }
                    }
                    catch (IOException | ClassNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                }
                else if (id == R.id.action_copy)
                {
                    WaveTrack copiedArea = null;
                    try
                    {
                        copiedArea = selectedTrack.copy(start, end);
                    }
                    catch (IOException | ClassNotFoundException e)
                    {
                        e.printStackTrace();
                        return true;
                    }

                    if(copiedArea != null)
                    {
                        bufferedTrack = copiedArea;
                        String message = ") Samples: " + copiedArea.getEndSample();
                        Helper.showCuteToast(ProjectActivity.this, message, Gravity.CENTER, Toast.LENGTH_LONG);
                        Log.e("PrAct: ", message);
                    }
                    else Helper.showCuteToast(ProjectActivity.this, "(", Gravity.CENTER, Toast.LENGTH_SHORT);
                }
                else if (id == R.id.action_paste)
                {
                    long cachedSamples = selectedTrack.getEndSample();

                    if(bufferedTrack == null) return false;
                    try
                    {
                        if(selectedTrack.paste(start, bufferedTrack))
                        {
                            player.initialize(false);
                            tracksFragment.setSelection(new MainAreaFragment.SampleRange(0,1));
                            tracksFragment.demandUpdate();
                            String message = "). Old samples: " + cachedSamples + ", New samples: " + selectedTrack.getEndSample();
                            Helper.showCuteToast(ProjectActivity.this, message, Gravity.CENTER, Toast.LENGTH_LONG);
                            Log.e("PrAct: ", message);
                        }
                        else Helper.showCuteToast(ProjectActivity.this, "(", Gravity.CENTER, Toast.LENGTH_SHORT);
                    }
                    catch (IOException | ClassNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    //EffectsHostFragment fragment = (EffectsHostFragment) createNewDialog(R.id.effectsFragment, EffectsHostFragment.class, true);
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    Fragment prev = getSupportFragmentManager().findFragmentById(R.id.effectsFragment);
                    if (prev != null) ft.remove(prev);
                    ft.addToBackStack(null);
                    // Create and show the dialog.

                    EffectsHostFragment hostFragment = EffectsHostFragment.newInstance(selectedTrack, project, tracksFragment.getSelection());
                    //Helper.showCuteToast(ProjectActivity.this, selectedTrack.getName());
                    hostFragment.show(ft, "Effects dialog");
                    // force to create views
                    getSupportFragmentManager().executePendingTransactions();
                    //hostFragment.setTrack(selectedTrack);
                }
                break;
            }
        }
        return true;
    }

    SupportedLanguages currentLanguage;
    DrawerLayout drawer;
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_newTrack)
        {
            WaveTrack recorderTrack = new WaveTrack(getResources().getString(R.string.name), project.getFileManager());
            recorderTrack.addClip(new Clip(project.getFileManager(), project.getProjectAudioInfo()));
            project.addTrack(recorderTrack);
        }
        if (id == R.id.nav_import)
        {
           //drawer.closeDrawer(GravityCompat.END);
            this.onBackPressed();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment prev = getSupportFragmentManager().findFragmentById(R.id.SuperAudioChooser);
            if (prev != null)  ft.remove(prev);
            ft.addToBackStack(null);
            // Create and show the dialog.
            AudioChooserFragment newFragment = AudioChooserFragment.newInstance();
            newFragment.show(ft, "Song chooser dialog");
        }
        else if (id == R.id.nav_export)
        {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment prev = getSupportFragmentManager().findFragmentById(R.id.ExportSongLayout);
            if (prev != null)  ft.remove(prev);
            ft.addToBackStack(null);
            // Create and show the dialog.
            ExportSongFragment newFragment = ExportSongFragment.newInstance(project.getName());
            newFragment.show(ft, "Song chooser dialog");
        }
        else if (id == R.id.nav_language)
        {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment prev = getSupportFragmentManager().findFragmentById(R.id.LanguageChooserLayout);
            if (prev != null)  ft.remove(prev);
            ft.addToBackStack(null);
            // Create and show the dialog.
            LanguageFragment newFragment = LanguageFragment.newInstance(currentLanguage.toString());
            newFragment.show(ft, "Song chooser dialog");
            newFragment.setFragmentOwner(this);
        }
        else if (id == R.id.nav_close)
        {
            Intent intent = new Intent(this, ProjectStartActivity.class);
            startActivity(intent);
            finish();
        }
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // refresh your views here
        super.onConfigurationChanged(newConfig);
    }

    public void setLanguage(SupportedLanguages language)
    {
        currentLanguage = language;
        Locale locale = new Locale("en");
        if(language.equals(SupportedLanguages.Русский)) locale = new Locale("ru");
        if(language.equals(SupportedLanguages.Українська)) locale = new Locale("uk");
        Locale.setDefault(locale);
        Configuration configuration = new Configuration();
        configuration.locale = locale;
        getBaseContext().getResources().updateConfiguration(configuration, null);

        // выводим английский текст на русской локали устройства
        setTitle(R.string.app_name);

        //onConfigurationChanged(configuration);

        //save data obout favorit language
        SharedPreferences pref = getSharedPreferences("info", MODE_PRIVATE);
        //Using putXXX - with XXX is type data you want to write like: putString, putInt...   from      Editor object
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("language", currentLanguage.toString());
        //finally, when you are done saving the values, call the commit() method.
        editor.commit();

        /*Intent intent = new Intent(this, ProjectStartActivity.class);
        startActivity(intent);
        finish();*/

        Intent intent = new Intent(this, ProjectActivity.class);
        intent.putExtra("openProjectWithName", project.getName());
        this.finish();
        startActivity(intent);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.Sacha:
            {
                if(player == null) break;

                if(player.isPlaying()) player.pause();
                else player.play();
                break;
            }
            case R.id.bRecorderStart:
            {
                if(player == null) break;

                WaveTrack recorderTrack = new WaveTrack(getResources().getString(R.string.name), project.getFileManager());
                recorderTrack.addClip(new Clip(project.getFileManager(), project.getProjectAudioInfo()));
                project.addTrack(recorderTrack);
                player.startRecording(recorderTrack);
                break;
            }
            case R.id.bStop:
            {
                if(player == null) break;

                player.stop();
                break;
            }
            case R.id.settings:
            {
                final ListPopupWindow popupWindow = new ListPopupWindow(this);

                PopupSeekbarAdapter adapter = new PopupSeekbarAdapter();
                ArrayList<String> list = new ArrayList<String>();
                list.add("Zoom");
               //list.add("Super Option");

                ArrayList<Integer> progressions = new ArrayList<Integer>();
                progressions.add((int)((float)tracksFragment.getSamplesPerPixel()/WaveTrackView.MAX_SAMPLES_PER_PIXEL * 10000f));
                progressions.add(0);

                adapter.setSeekBarListener(new PopupSeekbarAdapter.SeekBarListener()
                {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser, int positionInList)
                    {
                        if(positionInList == 0) // zoom
                        {
                            int samples = (int) (progress/10000f * WaveTrackView.MAX_SAMPLES_PER_PIXEL + 1);
                            tracksFragment.setSamplesPerPixel(samples);
                            //Log.e("Zoom", samples+"");
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar, int positionInList)
                    {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar, int positionInList)
                    {
                        //popupWindow.dismiss();
                    }
                });

                popupWindow.setAdapter(adapter.getAdapter(this, list, progressions, "Hell yeah!"));

                popupWindow.setModal(true);
                popupWindow.setAnchorView(v);

                Drawable background = new ColorDrawable(getResources().getColor(android.R.color.transparent));
                popupWindow.setBackgroundDrawable(background);

                popupWindow.setWidth((int) getResources().getDimension(R.dimen.popup_width));
                popupWindow.setAnimationStyle(android.R.style.Animation_InputMethod);
                popupWindow.setVerticalOffset(7);
                popupWindow.setHorizontalOffset(-5);

                popupWindow.show();

                break;
            }
        } // switch
    }

    BasePlayer.AudioPlayerListener playerListener = new BasePlayer.AudioPlayerListener()
    {
        @Override
        public void onInitialized(float totalTime)
        {
            progressView.setMax(totalTime);
            //progressView.setCurrent(0f);
        }

        @Override
        public void onStartRecording()
        {
            playPauseButt.setImageResource(R.drawable.ic_pause);
            bRecorderStart.setEnabled(false);
        }

        @Override
        public void onPlay()
        {
            playPauseButt.setImageResource(R.drawable.ic_pause);
            unlockEditing(false);
        }

        @Override
        public void onPause()
        {
            playPauseButt.setImageResource(R.drawable.ic_play);
            unlockEditing(true);
        }

        @Override
        public void onStop()
        {
            playPauseButt.setImageResource(R.drawable.ic_play);
            progressView.setCurrent(0f);
            bRecorderStart.setEnabled(true);
            unlockEditing(true);

            player.initialize(false);
        }

        @Override
        public void onCompleted()
        {
            progressView.setCurrent(0);
            playPauseButt.setImageResource(R.drawable.ic_play);
            unlockEditing(true);
        }
    };

    public void unlockEditing(boolean unlock)
    {
        if(unlock)
        {
            action_effects.setEnabled(true);
            action_select.setEnabled(true);
            action_editing.setEnabled(true);
        }
        else
        {
            action_effects.setEnabled(false);
            action_select.setEnabled(false);
            action_editing.setEnabled(false);
        }
    }

    private Project.ProjectListener projectListener = new Project.ProjectListener()
    {
        @Override
        public void onCreate(Project project)
        {
            ProjectActivity.this.project = project;

            // creates tracks fragment
            tracksFragment = MainAreaFragment.newInstance(project);
            tracksFragment.setGlobalPlayer(player);
            tracksFragment.setListener(trackAreaListener);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mainAreaContainer, tracksFragment)
                    .commit();

            // force to create views
            getSupportFragmentManager().executePendingTransactions();

            // --------------------------------------AUDIO STUFF-------------------------------------------------
            player = new AudioIO(ProjectActivity.this, project);
            player.addPlayerListener(playerListener);
            player.setTracks(project.getTracks());

            // timer to update the display
            updateTimer = new Runnable()
            {
                public void run()
                {
                    if (!isDragging && player.isPlaying()) progressView.setCurrent((float) player.getProgress());
                /*int bufferSize = 1024;
                ByteBuffer audioData = ByteBuffer.allocateDirect(bufferSize*4);
                audioData.order(ByteOrder.LITTLE_ENDIAN); // little-endian byte order
                BASS.BASS_ChannelGetData(player.getChannelHandle(), audioData, bufferSize*4);
                float[] pcm = new float[bufferSize]; // allocate an array for the sample data
                audioData.asFloatBuffer().get(pcm);

                // make object array
                Float []res = new Float[pcm.length];
                for(int i = 0; i < pcm.length; i++)
                    res[i] = pcm[i];

                // pass new data
                visualizerView.updateVisualizer(res);*/

                /*if(player.isRecording())
                {
                    final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(512);
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                    int len = BASS.BASS_ChannelGetData(player.getRecorderTrack().getChannel(), byteBuffer, 512);

                    if(len > 0)
                    {
                        final int floatLen = len / 2;
                        final Float[] floatBuff = new Float[floatLen];
                        for (int a = 0; a < floatLen; a++)
                        {
                            float num = byteBuffer.getShort(a) / 32768.f;
                            floatBuff[a] = num;

                            //Log.e("Sample: ", num+"");
                        }
                        visualizerView.updateVisualizer(floatBuff);
                    }
                }*/
                statusHandler.postDelayed(this, 50);
                }
            };
            statusHandler.postDelayed(updateTimer, 50);
        }

        @Override
        public void onOpenError(Project project)
        {
            finish();
        }

        @Override
        public void onUpdate(Project project)
        {
        }

        @Override
        public void onTrackRemoved(Project project, WaveTrack track)
        {
            player.setTracks(project.getTracks());
            player.reinitialize(false);
        }

        @Override
        public void onTrackAdded(Project project, WaveTrack track)
        {
            player.setTracks(project.getTracks());
            player.reinitialize(false);
        }

        @Override
        public void onClose(Project project)
        {
            tracksFragment.clearTracks();
        }
    };

    private MainAreaFragment.TrackFragmentListener trackAreaListener = new MainAreaFragment.TrackFragmentListener()
    {
        @Override
        public void onSelectionChanged(MainAreaFragment.SampleRange newSelection)
        {
            double start = tracksFragment.getSelectionStartTime();
            double end = tracksFragment.getSelectionEndTime();

            selectionText.setText("S:" + Helper.round(start, 2) + ", E:" + Helper.round(end, 2));
        }

        @Override
        public void onZoomChanged(int samplesPerPixel)
        {
            zoomText.setText("S/P: " + samplesPerPixel);
        }
    };

    // ---------------------------------SONG CHOOSER LISTENER INTERFACE----------------------------------

    @Override
    public void onOk(ArrayList<AudioFile> selectedAudioFiles)
    {
        if(selectedAudioFiles.size() == 0) return;

        AudioImporter importer = AudioImporter.getInstance();
        importer.setProject(project);
        importer.setListener(importerListener);

        AudioImporter.ImporterQuery[] queries = new AudioImporter.ImporterQuery[selectedAudioFiles.size()];

        // add all of the selected sound files to queue
        for (int i = 0; i <selectedAudioFiles.size(); i++)
        {
            AudioFile f = selectedAudioFiles.get(i);
            String destPath = SuperApplication.getAppDirectory() + "/" + f.getTitle();
            queries[i] = new AudioImporter.ImporterQuery(f, destPath);
            //tracksFragment.addTrack(f.getPath(), R.dimen.audio_track_default_width, R.dimen.audio_track_default_height);
        }
        // start loading
        importer.addToQueue(queries);

        // notify about process
        Helper.showCuteToast(ProjectActivity.this, R.string.import_notification, Gravity.CENTER, Toast.LENGTH_SHORT);
    }

    @Override
    public void onCancel(){}

    private AudioImporter.AudioImporterListener importerListener = new AudioImporter.AudioImporterListener()
    {
        NotificationManager notificationManager;
        NotificationCompat.Builder builder;
        int count = 0, totalFiles = 0; // number of imported files

        @Override
        public void onBegin(Queue<AudioImporter.ImporterQuery> queries)
        {
            count = 0;
            totalFiles = queries.size()+1;

            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // remove the notification in case it's there
            notificationManager.cancel(notificationId);

            // custom notification layout
            RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.import_notification);
            contentView.setImageViewResource(R.id.image, R.drawable.app_icon);
            contentView.setTextViewText(R.id.title, "Audio Import");
            contentView.setTextViewText(R.id.text, "Import progress: 1/" + totalFiles);

            builder = new NotificationCompat.Builder(ProjectActivity.this)
                    .setSmallIcon(R.drawable.app_icon)
                    .setAutoCancel(true)
                    .setContent(contentView);

            // allows to focus activity on click
            Intent activityIntent = new Intent(ProjectActivity.this, ProjectActivity.class);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pIntent = PendingIntent.getActivity(ProjectActivity.this, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pIntent);

            Notification notification = builder.build();
            notification.flags |= Notification.FLAG_NO_CLEAR;
            notification.defaults |= Notification.DEFAULT_LIGHTS;
            notificationManager.notify(notificationId, notification);

            // tip
            //Toast.makeText(ProjectActivity.this, R.string.import_notification, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onProgressUpdate(AudioImporter.ImporterQuery query, int progress, Clip clip)
        {
            RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.import_notification);
            contentView.setImageViewResource(R.id.image, R.drawable.app_icon);
            contentView.setTextViewText(R.id.title, "Audio Import");
            contentView.setTextViewText(R.id.text, "Import progress: " + (count+1) + "/" + totalFiles);
            contentView.setProgressBar(R.id.progressBar, 100, progress, false);

            Notification notification = builder.setContent(contentView).build();
            notification.flags |= Notification.FLAG_NO_CLEAR; // non cancellable notificaion
            notificationManager.notify(notificationId, notification);

            // audio loaded
            if(progress >= 100)
            {
                WaveTrack track = new WaveTrack(query.audioFileInfo.getTitle(), project.getFileManager());
                track.addClip(clip);

                project.addTrack(track);
                count++;
            }
        }

        @Override
        public void onFinish(ArrayList<AudioImporter.ImporterQuery> succeededQueries, ArrayList<AudioImporter.ImporterQuery> failedQueries)
        {
            RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.import_notification);
            contentView.setImageViewResource(R.id.image, R.drawable.app_icon);
            contentView.setTextViewText(R.id.title, "Importing has been complete!");

            String detailedMessage = "Succeeded: " + succeededQueries.size();
            if(failedQueries.size() > 0)
                detailedMessage += "\nFailed: " + failedQueries.size();

            contentView.setTextViewText(R.id.text, detailedMessage);

            // hide progress bar
            contentView.setViewVisibility(R.id.progressBar, View.GONE);

            Notification notification = builder.setContent(contentView).build();
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notification.defaults |= Notification.DEFAULT_VIBRATE;
            notificationManager.notify(notificationId, notification);

            // remove listener
            AudioImporter.getInstance().setListener(null);

            player.initialize(false);
        }
    };


    // ---------------------------------Effects Host LISTENER INTERFACE----------------------------------

    @Override
    public void onEffectsConfirmed()
    {
        project.updateProjectFile();
    }

    @Override
    public void onEffectsCancelled()
    {

    }

    // ----- permissions ----------
    public static boolean hasPermissions(Context context, String... permissions)
    {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null)
        {
            for (String permission : permissions)
            {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        switch (requestCode)
        {
            case 1:
            {
                for(int i = 0; i < permissions.length; i++)
                {
                    if(permissions[i].equals(Manifest.permission.RECORD_AUDIO))
                    {
                        // If request is cancelled, the result arrays are empty.
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
                        {
                            // permission was granted, yay!
                        }
                        else
                        {
                            finish();
                            // permission denied, boo! Disable the functionality that depends on this permission.
                        }
                    }
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    //get result from ExportSongFragment
    @Override
    public void onOk(String name, String album, String year, String format)
    {
        String pathStorageMusic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();

        Helper.checkDirectory(pathStorageMusic);
       // Toast toast = Toast.makeText(ProjectActivity.this, pathStorageMusic, Toast.LENGTH_LONG);
       // toast.setGravity(Gravity.CENTER, 0, 0);
       // toast.show();

        AudioExporter exporter = new AudioExporter(this, getResources().getString(R.string.encoding), project, name, album, year, format, pathStorageMusic);
        exporter.execute();
    }
}

