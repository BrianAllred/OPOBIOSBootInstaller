package com.frozeninferno.opobiosbootanimation;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by Brian on 12/16/13.
 */
public class PreviewFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    byte[] buffer = new byte[1024];

    public PreviewFragment() {
    }

    //used to save titleCase, possibly unnecessary...?
    public static PreviewFragment newInstance(int sectionNumber) {
        PreviewFragment fragment = new PreviewFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //inflate the preview layout
        View rootView = inflater.inflate(R.layout.fragment_preview, container, false);
        //titleCase gets decremented sometimes and a negative titleCase is
        //an unexpected state, so take care of it
        if (MainActivity.titleCase < 0) {
            MainActivity.titleCase = 0;
        }
        //determine which fragment to actually create
        switch (MainActivity.titleCase) {
            case 0:
                modelPage(rootView);
                break;
            case 1:
                framesPage(rootView);
                break;
            case 2:
                installFrag();
                break;
        }
        return rootView;
    }

    public void modelPage(View rootView) {
        String[] models;
        ArrayAdapter<String> adapter;
        ListView modelsList;
        models = getResources().getStringArray(R.array.opomodels);
        //get models strings into string adapter. Easier with String[], but less code and cleaner with
        //adapter
        adapter = new ArrayAdapter<>(getActivity(), R.layout.list_item, models);
        modelsList = (ListView) rootView.findViewById(R.id.ListView);
        modelsList.setAdapter(adapter);
        //listen for the click
        modelsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                //save choice
                MainActivity.modelChoice = position;
                //increment titleCase
                MainActivity.titleCase += 1;
                //recreate this fragment, which will result in the next "variation" of the fragment,
                //since titleCase was incremented
                recreateFrag();
            }
        });
    }

    public void framesPage(View rootView) {
        final SeekBar s = (SeekBar) rootView.findViewById(R.id.seekBar);
        final TextView fpsText = (TextView) rootView.findViewById(R.id.fpsText);
        TextView fpsAbout = (TextView) rootView.findViewById(R.id.fpsAbout);
        CheckBox checkBox = (CheckBox) rootView.findViewById(R.id.checkbox_force_play);
        checkBox.setChecked(MainActivity.forceChoice);
        checkBox.setVisibility(View.VISIBLE);
        fpsText.setText(Integer.toString(MainActivity.frameChoice));
        fpsText.setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.framesText).setVisibility(View.VISIBLE);
        fpsAbout.setText(R.string.about_fps);
        fpsAbout.setVisibility(View.VISIBLE);
        s.setProgress(MainActivity.frameChoice - 5);
        s.setVisibility(View.VISIBLE);
        Button preview = (Button) rootView.findViewById(R.id.previewButton);
        preview.setVisibility(View.VISIBLE);
        preview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!MainActivity.filesExtracted) {
                    final ProgressDialog extractProgress = ProgressDialog.show(getActivity(), "Please wait...", "Preparing files (this may take a while)...", true, false);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            FileDirManager.extractImages(getActivity().getAssets());
                            File imageZip = new File(FileDirManager.ExternalFilesPath + "/images.zip");
                            if (imageZip.exists() && imageZip.canRead() && FileDirManager.ExternalFilesDir().canWrite() && Unpack(imageZip))
                                MainActivity.filesExtracted = true;
                            extractProgress.dismiss();
                        }
                    }).start();
                }
                MainActivity.titleCase += 1;
                recreateFrag();
            }
        }
        );
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar,
                                          int progress, boolean fromUser) {
                //Progress bars always use 0-max, so if the purpose of the bar requires
                //a minimum greater than 0, need an offset.
                int adjProgress = progress + 5;
                //update text that informs user of bar state
                fpsText.setText(Integer.toString(adjProgress));
                MainActivity.frameChoice = adjProgress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        //call onAttach required to prevent crashes
        super.onAttach(activity);
        //call onSectionAttached in order to update title on actionbar
        ((MainActivity) activity).onSectionAttached(MainActivity.titleCase + 1);
    }

    //recreate this fragment, used to create next iteration
    public void recreateFrag() {
        onAttach(getActivity());
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment newFrag = new PreviewFragment();
        getFragmentManager().beginTransaction();
        transaction.replace(getId(), newFrag);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    //create the installation fragment.
    //kept these fragments separate since there's a *lot* of code in the
    //installation fragment.
    public void installFrag() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment newFrag = new InstallFragment();
        getFragmentManager().beginTransaction();
        transaction.replace(getId(), newFrag);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private boolean Unpack(File imageZip) {
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(imageZip));
            ZipEntry ze = zis.getNextEntry();
            while(ze != null){
                String fileName = ze.getName();
                File newFile = new File(FileDirManager.ExternalFilesPath + File.separator + fileName);

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                if(ze.isDirectory()) {
                    newFile.mkdirs();
                }
                else {
                    new File(newFile.getParent()).mkdirs();
                    FileOutputStream fos = new FileOutputStream(newFile);

                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }

                    fos.close();
                }
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
            return true;
        }catch(FileNotFoundException e){
            Log.e("Zip error", e.toString());
        }
        catch(IOException e){
            Log.e("Zip error", e.toString());
        }
        return false;
    }

}
