package com.sunflower.catchtherainbow.Adapters;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sunflower.catchtherainbow.AudioClasses.AudioFile;
import com.sunflower.catchtherainbow.Helper;
import com.sunflower.catchtherainbow.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alexandr on 05.02.2017.
 */

public class AudioFilesAdapter extends CursorAdapter implements
        LoaderManager.LoaderCallbacks<Cursor>
{
    private final Activity context;
    private HashMap<Long, Boolean> selection = new HashMap<>();
    private String filter = "";
    private LayoutInflater inflater;
    private int selectedCount = 0;

    private String searchQuery = "";
    private String sortOrder = "";

    private int loaderId;


    public AudioFilesAdapter(final Activity context, int loaderId, int flags)
    {
        super(context, null, flags);
        this.context = context;

        this.loaderId = loaderId;

        context.getLoaderManager().restartLoader(loaderId, new Bundle(), this);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getSelectedCount()
    {
        return selectedCount;
    }

    public ArrayList<AudioFile> getSelectionAudioFiles()
    {
        ArrayList<AudioFile> selectedAudio = new ArrayList<>();
        for (Map.Entry<Long, Boolean> entry : selection.entrySet())
        {
            Long key = entry.getKey();
            Boolean value = entry.getValue();
            if (value == true)
            {
                selectedAudio.add(getSongFromPosition(key.intValue()));
            }
        }
        return selectedAudio;
    }

    public void filterAllAudioFiles(String filter)
    {
        SetSelectAll(false); //снять выделение

        searchQuery = filter;
        if(loader != null)
            loader.setSearchQuery(filter);
        context.getLoaderManager().getLoader(loaderId).forceLoad();

        ///////
        /*changeCursor(Helper.getSongsAudioCursor(context, filter, sortOrder));
        this.filter = filter;
        notifyDataSetChanged();*/
    }


    boolean isASC = true;
    public void SetSortOrder(String sortOrder)
    {
        String sort = sortOrder;
        if(this.sortOrder.equals(sortOrder))
        {
            if(isASC)
            {
                sort += " DESC";
                isASC = false;
            }
            else isASC = true;
        }
        else isASC = true;

        this.sortOrder = sortOrder;
        if(loader != null)
            loader.setOrder(sort);
        context.getLoaderManager().getLoader(loaderId).forceLoad();

        SetSelectAll(false);

        /*changeCursor(Helper.getSongsAudioCursor(context, filter, sortOrder));
        this.sortOrder = sortOrder;
        notifyDataSetChanged();*/
    }

    public ArrayList<AudioFile> getAllAudioFiles(String filter)
    {
        ArrayList<AudioFile> selectedAudio = new ArrayList<>();
        for (Map.Entry<Long, Boolean> entry : selection.entrySet())
        {
            Long key = entry.getKey();
            Boolean value = entry.getValue();
            selectedAudio.add(getSongFromPosition(key.intValue()));
        }
        changeCursor(Helper.getSongsAudioCursor(context, filter, sortOrder));
        notifyDataSetChanged();
        return selectedAudio;
    }

    public void setNewSelection(Long id)
    {
        if (isPersonChecked(id))
        {
            selection.put(id, false);
            selectedCount--;
        }
        else
        {
            selection.put(id, true);
            selectedCount++;
        }
        notifyDataSetChanged();
    }

    public void SetSelectAll(boolean value)
    {
        Cursor cursor = getCursor();

        if(cursor == null) return;

        HashMap<Long, Boolean> newSelection = new HashMap<>();
        selectedCount = 0;
        for (int i = 0; i < cursor.getCount(); i++)
        {
            cursor.moveToPosition(i);
            newSelection.put((long) i, value);
            selectedCount++;
        }
        if (value == false) selectedCount = 0;
        selection = newSelection;
        notifyDataSetChanged();
    }

    public boolean isPersonChecked(Long row)
    {
        Boolean result = selection.get(row);
        return result == null ? false : result;
    }

    public void notifyDataSetChanged()
    {
        super.notifyDataSetChanged();
    }

    public AudioFile getSongFromPosition(int position)
    {
        Cursor cur = getCursor();
        if(cur == null) return null;

        cur.moveToPosition(position);
        return Helper.getAudioFileByCursor(context, cur);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
        return inflater.inflate(R.layout.item_audio, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor)
    {
        View rowView;
        final AudioFile audioFile = getSongFromPosition(cursor.getPosition());
        //if(!audioFile.getTitle().contains("Track")) return;
        // if the view isn't generated
        if (view == null)
        {
            rowView = inflater.inflate(R.layout.item_audio, null, true);
        }
        else rowView = view;

        View itemView = rowView.findViewById(R.id.music_layout);

        // ----- Tint -----
        if (isPersonChecked((long) cursor.getPosition()))
            itemView.setBackgroundColor(context.getResources().getColor(R.color.selectedListItem));
        else
            itemView.setBackgroundColor(context.getResources().getColor(R.color.backgroundListView));
        // -----Tint-end------

        ImageView albumImage = (ImageView) rowView.findViewById(R.id.imageView);
        TextView nameLayout = (TextView) rowView.findViewById(R.id.tvName);
        TextView artistLayout = (TextView) rowView.findViewById(R.id.tvArtist);
        TextView timesLayout = (TextView) rowView.findViewById(R.id.tvTimes);

        if (audioFile.getBitmapImage() != null)
            albumImage.setImageBitmap(audioFile.getBitmapImage());

        nameLayout.setText(audioFile.getTitle());
        artistLayout.setText(audioFile.getArtist());
        timesLayout.setText(Helper.secondToString(audioFile.getDuration()));
    }


    //------------------------------------------------For background loading----------------------------

    private SuperAudioCursorLoader loader;
    // ---------------loader listener-------------
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        loader = new SuperAudioCursorLoader(context, searchQuery, sortOrder);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data)
    {
        changeCursor(data);
        this.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        changeCursor(null);
        this.notifyDataSetChanged();
    }
}

class SuperAudioCursorLoader extends CursorLoader
{
    private Context context;
    private String order = "title";
    private String searchQuery = "state";

    public SuperAudioCursorLoader(Context context, String searchQuery, String order)
    {
        super(context);
        this.searchQuery = searchQuery;
        this.order = order;
        this.context = context;
    }

    // Загрузка данных из БД в отдельном потоке
    @Override
    public Cursor loadInBackground()
    {
        Cursor cursor = Helper.getSongsAudioCursor(context, searchQuery, order);
        return cursor;
    }

    public String getOrder()
    {
        return order;
    }

    public void setOrder(String order)
    {
        this.order = order;
    }

    public String getSearchQuery()
    {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery)
    {
        this.searchQuery = searchQuery;
    }
}
    // ---------------loader listener end-------------

   /* public void refeshData()
    {
        updateFilter(filter);
        getFilter().filter(filter);
    }

    public void updateFilter(String newFilter)
    {
        if(newFilter == null || newFilter.equals(filter)) return;

        filter = newFilter;
        //changeCursor(sqlHelper.fetchPeople(filter));

        notifyDataSetChanged();
    }

    public void invertSelection(Long id)
    {
        if(selection.containsKey(id))
        {
            boolean sel = selection.get(id);
            selection.put(id, !sel);
        }
        else selection.put(id, true);

        notifyDataSetChanged();
    }

    public void invertAll()
    {
        Cursor cursor = getCursor();
        if(!cursor.moveToPosition(0)) return;
        do
        {
            invertSelection(cursor.getLong(cursor.getColumnIndex(SuperDatabaseHelper.KEY_PERSON_ID)));
        }
        while(cursor.moveToNext());
    }

    public void setNewSelection(Long id, boolean value)
    {
        selection.put(id, value);
        notifyDataSetChanged();
    }

    public boolean isPersonChecked(Long person)
    {
        Boolean result = selection.get(person);
        return result == null ? false : result;
    }

    public void selectAll()
    {
        Cursor cursor = getCursor();

        for(int i = 0; i < cursor.getCount(); i++)
        {
            cursor.moveToPosition(i);
            Long id = cursor.getLong(cursor.getColumnIndex(SuperDatabaseHelper.KEY_PERSON_ID));
            setNewSelection(id, true);
        }
    }

    public ArrayList<AudioFile> getSelectedDudes(boolean includeDisplayedOnly)
    {
        ArrayList <AudioFile> res = new ArrayList<>();
        for(Long n : selection.keySet())
        {
            // get selection by ids
        }
        return res;
    }

    public ArrayList<Long> getSelectedIds()
    {
        ArrayList <Long> res = new ArrayList<>();
        for(Long n : selection.keySet())
        {
            if(selection.get(n) == true)
                res.add(n);
        }
        return res;
    }

    public void clearSelection()
    {
        selection = new HashMap<>();
        notifyDataSetChanged();
    }

    public void notifyDataSetChanged()
    {
        super.notifyDataSetChanged();
    }

    public AudioFile getSongFromPosition(int position)
    {
        Cursor cur = getCursor();
        cur.moveToPosition(position);
        return sqlHelper.getPersonById(cur.getLong(cur.getColumnIndex(SuperDatabaseHelper.KEY_PERSON_ID)));
    }

    /*public ArrayList<Long> getDisplayedDudesIds()
    {
        ArrayList<Long> res = new ArrayList<>();
        Cursor cursor = getCursor();
        cursor.moveToPosition(0);
        do
        {
            res.add(cursor.getLong(cursor.getColumnIndex(SuperDatabaseHelper.KEY_PERSON_ID)));
        }
        while(cursor.moveToNext());
        return res;
    }*/

