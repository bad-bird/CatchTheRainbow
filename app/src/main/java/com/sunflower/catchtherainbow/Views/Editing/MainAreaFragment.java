package com.sunflower.catchtherainbow.Views.Editing;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.sunflower.catchtherainbow.R;
import com.sunflower.catchtherainbow.Views.Editing.Waveform.soundfile.CheapSoundFile;
import com.sunflower.catchtherainbow.Views.Editing.Waveform.view.WaveformView;
import com.sunflower.catchtherainbow.Views.Helpful.Thumb;

import java.io.IOException;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MainAreaFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MainAreaFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainAreaFragment extends Fragment
{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private TableLayout tracksLayout;
    private ScrollView verticalScrollView;

    public MainAreaFragment()
    {
        //TableRow
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MainAreaFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MainAreaFragment newInstance(String param1, String param2)
    {
        MainAreaFragment fragment = new MainAreaFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_main_area, container, false);

        tracksLayout = (TableLayout)root.findViewById(R.id.tracks_layout);
        verticalScrollView = (ScrollView) root.findViewById(R.id.verticalScrollView);

       /* addTrack(350, 150);
        addTrack(350, 150);
        addTrack(350, 160);
        addTrack(100, 200);*/

        return root;
    }

    ProgressDialog progressDialog;
    TableRow trow;
    public void addTrack(final String path, int w, int h)
    {
        trow = new TableRow(getActivity());

        // needs to be set to be able to select track
        trow.setClickable(true);
        trow.setFocusable(true);
        trow.setFocusableInTouchMode(true);
        trow.setBackgroundResource(R.drawable.track_selector);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        trow.setLayoutParams(lp);
        trow.setPadding(1, 1, 1, 0);

        // header view
        TrackHeaderView head = new TrackHeaderView(getActivity());
        trow.addView(head, 0);

        TextView v = new TextView(getContext());
        v.setBackgroundColor(Color.RED);
        v.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT));
        trow.addView(v, 1);

         // Load the sound file in a background thread
        new LongOperation().execute(path);

        // add header row
        tracksLayout.addView(trow, 0);

        // thumb for resizing row
        TableRow thumbRow = new TableRow(getActivity());

        Thumb th = new Thumb(getActivity(), head, Thumb.ThumbKind.Vertical);

        thumbRow.addView(th, 0);

        // add thumb row
        tracksLayout.addView(thumbRow, 1);
    }

    private final Handler mHandler = new Handler();
    private CheapSoundFile file;

    private class LongOperation extends AsyncTask<String, Double, CheapSoundFile>
    {
        private long loadingLastUpdateTime;

        @Override
        protected CheapSoundFile doInBackground(String... params)
        {
            loadingLastUpdateTime = System.currentTimeMillis();
            CheapSoundFile.ProgressListener progressListener = new CheapSoundFile.ProgressListener()
            {
                @Override
                public boolean reportProgress(final double fractionComplete)
                {
                    long now = System.currentTimeMillis();
                    if (now - loadingLastUpdateTime > 100)
                    {
                        publishProgress(fractionComplete);
                        loadingLastUpdateTime = now;
                    }
                    return true;
                }
            };

            try
            {
                 file = CheapSoundFile.create(params[0], progressListener);
//                file.setProgressListener(null);
            }
            catch (IOException e)
            {
                Log.e("Tracks", "Error while loading sound file", e);
                progressDialog.dismiss();
                return null;
            }

            return file;
        }

        @Override
        protected void onPostExecute(CheapSoundFile result)
        {
            finishOpeningSoundFile(result, trow);
//            progressDialog.dismiss();
        }

        @Override
        protected void onPreExecute()
        {
            /*progressDialog = new ProgressDialog(getActivity());
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setTitle(R.string.progress_dialog_loading);
            progressDialog.setCancelable(false);
            progressDialog.show();*/
        }

        @Override
        protected void onProgressUpdate(Double... values)
        {
           // progressDialog.setProgress((int) (progressDialog.getMax() * values[0]));
        }
    }


    protected void finishOpeningSoundFile(final CheapSoundFile file, final TableRow tableRow)
    {
        mHandler.post(new Runnable()
        {
          @Override
          public void run()
          {
              WaveformView wave = new WaveformView(getActivity());
              wave.setPadding(2,2,2,2);
              wave.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT));
              tableRow.addView(wave, 1);
              //wave.setSoundFile(file);
              /*TextView v = new TextView(getContext());
              v.setBackgroundColor(Color.RED);
              v.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT));
              trow.addView(v, 1);*/
          }
        });

        // add track row

       // wave.invalidate();
    }


    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri)
    {
        if (mListener != null)
        {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        /*if (context instanceof OnFragmentInteractionListener)
        {
            mListener = (OnFragmentInteractionListener) context;
        }
        else
        {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener
    {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}