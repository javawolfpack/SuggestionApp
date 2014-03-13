package csci567.suggestionapp.app;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Vector;

//import android.widget.CheckBox;
//import android.widget.TextView;



public class MainActivity extends Activity implements ActionBar.TabListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    private ShareActionProvider mShareActionProvider;
    private ArrayAdapter<String> adapter;
    private ArrayAdapter<String> adapter_temp;
    private ListView listView1;
    private ListView listView2;
    private MenuItem menuItem;
    private ActionMode mActionMode;
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context_action, menu);
            try {
                /** Getting the actionprovider associated with the menu item whose id is share */
                mShareActionProvider = (ShareActionProvider) menu.findItem(R.id.menu_item_share).getActionProvider();

                /** Setting a share intent */
                mShareActionProvider.setShareIntent(getStringShareIntent(""));
            }
            catch(Exception e){
                e.printStackTrace();
            }

            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if(item.getItemId()== R.id.menu_item_share) {
                // Fetch and store ShareActionProvider
                //shareCurrentItem();
                mode.finish(); // Action picked, so close the CAB
                return true;
            }
            return false;
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
        }
    };

    /** Returns a share intent */
    private Intent getStringShareIntent(String text){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "SUBJECT");
        intent.putExtra(Intent.EXTRA_TEXT,text);
        return intent;
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }



    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        // Assumes current activity is the searchable activity
        final SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() <= 0) {
                    listView1.setAdapter(adapter);
                }
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                Vector<String> v = new Vector<String>();
                for(int i=0;i<adapter.getCount();i++){
                    if(adapter.getItem(i).contains(query)){
                        v.add(adapter.getItem(i));
                    }
                }
                String [] s = v.toArray(new String[v.size()]);
                adapter_temp = new ArrayAdapter<String>(getBaseContext(),
                        android.R.layout.simple_list_item_1, s);
                listView1.setAdapter(adapter_temp);
                //Toast.makeText(getBaseContext(), "Query: " + query, Toast.LENGTH_LONG).show();
                return true;
            }
        };
        searchView.setOnQueryTextListener(queryTextListener);

        /*MenuItem item =  menu.findItem(R.id.menu_item_share);
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();*/
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        /*if(id==R.id.menu_item_share){
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "TEXT");
            doShare(shareIntent);
        }*/
        if(id==R.id.action_refresh){
            //Refresh Data
            menuItem = item;
            menuItem.setActionView(R.layout.refresh_action);
            menuItem.expandActionView();
            new getData().execute();
            Toast.makeText(getBaseContext(), "Refreshing", Toast.LENGTH_LONG).show();
            return true;
        }
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;
        if(item.getItemId()==R.id.edit){
            Toast.makeText(getBaseContext(), "Edit: " + adapter.getItem(position), Toast.LENGTH_LONG).show();
            return true;
        }
        if(item.getItemId()==R.id.delete){
            Toast.makeText(getBaseContext(), "Delete: "+ adapter.getItem(position), Toast.LENGTH_LONG).show();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return new PlaceholderFragment(position);
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public class PlaceholderFragment extends Fragment {
        private int position;

        public PlaceholderFragment(int position) {
            this.position = position;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView=null;
            switch(position){
                case 0:
                    Log.d("SuggestionAPP","Case 0");
                    rootView = inflater.inflate(R.layout.fragment_main, container, false);
                    break;
                case 1:
                    Log.d("SuggestionAPP","Case 1");
                    rootView = inflater.inflate(R.layout.fragment_main2, container, false);
                    listView1= (ListView) rootView.findViewById(R.id.listView1);
                    registerForContextMenu(listView1);
                    new getData().execute();
                    break;
                case 2:
                    Log.d("SuggestionAPP","Case 2");
                    rootView = inflater.inflate(R.layout.fragment_main2, container, false);
                    listView2= (ListView) rootView.findViewById(R.id.listView1);
                    listView2.setLongClickable(true);
                    listView2.setOnItemLongClickListener(new OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> arg0, View view, final int position, long arg3) {
                            if (mActionMode != null) {
                                return false;
                            }

                            // Start the CAB using the ActionMode.Callback defined above
                            mActionMode = getActivity().startActionMode(mActionModeCallback);
                            setShareIntent(getStringShareIntent(listView2.getItemAtPosition(position).toString()));
                            //Toast.makeText(getBaseContext(), listView2.getItemAtPosition(position).toString(), Toast.LENGTH_LONG).show();
                            listView2.setItemChecked(position, true);
                            return true;

                        }
                    });

                    new getData().execute();
                    break;
            }
            return rootView;

        }
    }
    private class getData extends AsyncTask<Void, Void, Void> {
        InputStream inputStream = null;
        String result = "";
        //String url_select = "http://www.bryancdixon.com/androidjson";
        String url_select="http://www.bryancdixon.com/androidjson";
        Vector<String> results = new Vector<String>();



        protected void onPreExecute() {
            Log.d("SuggestionAPP ", "Preparing to get Suggestions");
        }

        protected Void doInBackground(Void... params) {
            try {

                URI uri = new URI(url_select);
                HttpClient httpclient = new DefaultHttpClient();
                HttpResponse httpResponse = httpclient.execute(new HttpGet(uri));
                HttpEntity httpEntity = httpResponse.getEntity();
                inputStream = httpEntity.getContent();
            } catch (UnsupportedEncodingException e1) {
                Log.e("UnsupportedEncodingException", e1.toString());
                e1.printStackTrace();
            } catch (ClientProtocolException e2) {
                Log.e("ClientProtocolException", e2.toString());
                e2.printStackTrace();
            } catch (IllegalStateException e3) {
                Log.e("IllegalStateException", e3.toString());
                e3.printStackTrace();
            } catch (IOException e4) {
                Log.e("IOException", e4.toString());
                e4.printStackTrace();
            } catch (URISyntaxException e) {
                Log.e("URISyntaxException ", e.toString());
                e.printStackTrace();
            }
            // Convert response to string using String Builder
            try {
                BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStream, "iso-8859-1"), 8);
                StringBuilder sBuilder = new StringBuilder();
                String line = null;
                while ((line = bReader.readLine()) != null) {
                    sBuilder.append(line + "\n");
                }
                inputStream.close();
                result = sBuilder.toString();

            } catch (Exception e) {
                Log.e("StringBuilding & BufferedReader", "Error converting result " + e.toString());
            }
            return null;
        }

        protected void onPostExecute(Void donothing) {
            //parse JSON data
            String text = "";
            try {
                JSONObject jO = new JSONObject(result);
                JSONArray jArray = jO.getJSONArray("suggestions");


                for(int i=0; i < jArray.length(); i++) {
                    JSONObject jObject = jArray.getJSONObject(i);
                    results.add(jObject.getString("text"));
                    text += jObject.getString("text")+"\n\n";
                    //Log.d("SuggestionAPP ",text);

                } // End Loop
                if(jArray.length()<=0){
                    results.add("No Suggestions");
                    text="No Suggestions";
                }
            } catch (JSONException e) {
                Log.e("JSONException", "Error: " + e.toString());
                results.add("No Suggestions");
                text="No Suggestions";
            }
            try {
                //Generate String Array from Vector
                String[] s = results.toArray(new String[results.size()]);
                adapter = new ArrayAdapter<String>(getBaseContext(),
                        android.R.layout.simple_list_item_1, s);
                listView1.setAdapter(adapter);
                listView2.setAdapter(adapter);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            if(menuItem!=null) {
                menuItem.collapseActionView();
                menuItem.setActionView(null);
            }
            //Method when using textview
            //txt.setText(text);

            //set TextView Contents to be JSON response
            //txt.setText(result);
        }
    }

}
