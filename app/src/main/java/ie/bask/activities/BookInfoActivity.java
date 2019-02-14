package ie.bask.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import ie.bask.PicassoTrustAll;
import ie.bask.R;
import ie.bask.main.Base;
import ie.bask.main.BookopediaApp;
import ie.bask.models.Book;


public class BookInfoActivity extends Base {
    private ImageView ivBookCover;
    private TextView tvTitle;
    private TextView tvAuthor;
    private TextView tvPublisher;
    private TextView tvPageCount;
    private TextView tvDescription;
    private Button btnToRead;
    private TextView tvDateAdded;
    private Menu menu;
    private TextView tvNotes;
    private View hrView;
    private TextView tvNotesLabel;
    private Button btnAddNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_info);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        app = (BookopediaApp) getApplication();
        ivBookCover = findViewById(R.id.ivBookCover);
        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvPublisher = findViewById(R.id.tvPublisher);
        tvPageCount = findViewById(R.id.tvPageCount);
        tvDescription = findViewById(R.id.tvDescription);
        btnToRead = findViewById(R.id.btnToRead);
        tvDateAdded = findViewById(R.id.tvDateAdded);
        tvNotes = findViewById(R.id.tvNotes);
        hrView = findViewById(R.id.hrView2);
        tvNotesLabel = findViewById(R.id.tvNotesLabel);
        btnAddNotes = findViewById(R.id.btnAddNotes);

        // Use the book to populate the data into our views
        final Book book = (Book) getIntent().getSerializableExtra("book_info_key");
        loadBook(book);

        // Listen for clicks
        setBookToReadListener(book);
        setAddNoteListener(book);
    }

    // Populate data for the book
    private void loadBook(Book book) {
        if (book.getDateAdded() == null) {
            //change activity title
            this.setTitle(book.getTitle());
            btnToRead.setVisibility(View.VISIBLE);
            hrView.setVisibility(View.VISIBLE);
            tvDateAdded.setVisibility(View.GONE);
            tvNotesLabel.setVisibility(View.GONE);
            btnAddNotes.setVisibility(View.GONE);
            tvNotes.setVisibility(View.GONE);

            // Populate data
            PicassoTrustAll.getInstance(getApplicationContext())
                    .load(Uri.parse(book.getImageLink()))
                    .fit()
                    .centerInside()
                    .error(R.drawable.ic_nocover)
                    .into(ivBookCover);
            tvTitle.setText(book.getTitle());
            tvAuthor.setText(book.getAuthor());
            tvPublisher.setText(String.format(getResources().getString(R.string.publisher), book.getPublisher()));
            tvPageCount.setText(String.format(getResources().getString(R.string.page_count), book.getNumPages()));
            tvDescription.setText(book.getDescription());
        } else {
            // Change activity title
            this.setTitle(book.getTitle());
            // Hide/show widgets
            btnToRead.setVisibility(View.GONE);
            hrView.setVisibility(View.GONE);
            tvDateAdded.setVisibility(View.VISIBLE);
            btnAddNotes.setVisibility(View.VISIBLE);
            tvNotes.setVisibility(View.VISIBLE);

            // Populate data
            PicassoTrustAll.getInstance(getApplicationContext())
                    .load(Uri.parse(book.getImageLink()))
                    .fit()
                    .centerInside()
                    .error(R.drawable.ic_nocover)
                    .into(ivBookCover);
            tvTitle.setText(book.getTitle());
            tvAuthor.setText(book.getAuthor());
            tvPublisher.setText(String.format(getResources().getString(R.string.publisher), book.getPublisher()));
            tvPageCount.setText(String.format(getResources().getString(R.string.page_count), book.getNumPages()));
            tvDescription.setText(book.getDescription());
            tvDateAdded.setText(String.format(getResources().getString(R.string.added_on), book.getDateAdded()));
            if (!book.getNotes().equals("0")) {
                tvNotes.setText(book.getNotes());
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final MenuItem toReadItem = menu.findItem(R.id.action_to_read);
        final MenuItem clearBooksItem = menu.findItem(R.id.action_clear);
        final MenuItem deleteBookItem = menu.findItem(R.id.action_delete);

        // Hide menu items
        if (getIntent().getStringExtra("ToReadContext") == null) {
            deleteBookItem.setVisible(false);
        }
        searchItem.setVisible(false);
        clearBooksItem.setVisible(false);
        if (app.booksToRead.isEmpty()) {
            toReadItem.setVisible(false);
        }

        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case (R.id.action_home):
                app.booksResults.clear();
                Intent goHome = new Intent(getApplicationContext(), BookListActivity.class);
                startActivity(goHome);
                break;
            case (R.id.action_to_read):
                Intent toReadIntent = new Intent(getApplicationContext(), ToReadBooksActivity.class);
                startActivity(toReadIntent);
                break;
            case (R.id.action_delete):
                // Remove book from Firebase Database and local ArrayList
                Book book = (Book) getIntent().getSerializableExtra("book_info_key");
                app.booksToRead.remove(book);
                app.booksToReadDb.child(book.getBookId()).removeValue();
                app.booksToReadDb.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            finishAffinity();
                            startActivity(new Intent(getApplicationContext(), BookListActivity.class));
                        } else {
                            finishAffinity();
                            startActivity(new Intent(getApplicationContext(), ToReadBooksActivity.class));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("Bookopedia", "" + databaseError);
                    }
                });

                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setBookToReadListener(final Book book) {
        btnToRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Use the book to populate the data into our views
                String currentDate = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
                Book bookToRead = new Book(book.getBookId(), book.getAuthor(), book.getTitle(), book.getImageLink(),
                        book.getDescription(), book.getPublisher(), book.getNumPages(), currentDate, "0");
                addBookToRead(bookToRead);
                MenuItem toReadItem = menu.findItem(R.id.action_to_read);
                toReadItem.setVisible(true);
            }
        });
    }

    private void addBookToRead(Book book) {
        // Adding the book to Firebase Database
        app.booksToReadDb.child(book.getBookId()).setValue(book);
        app.booksToRead.add(book);
        Toast.makeText(getApplicationContext(), "Book added to TO READ list", Toast.LENGTH_SHORT).show();
    }

    public void setAddNoteListener(final Book book) {
        tvNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater li = LayoutInflater.from(BookInfoActivity.this);
                final View promptsView = li.inflate(R.layout.alert_dialog, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(BookInfoActivity.this);
                // Set layout alert_dialog.xml to AlertDialog
                alertDialogBuilder.setView(promptsView);

                EditText userInput = promptsView.findViewById(R.id.etUserInput);
                userInput.setText(book.getNotes().equals("0") ? "" : tvNotes.getText().toString());
                userInput.requestFocus();

                // Set dialog messages
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Get user input and set it to tvNotes
                                EditText userInput = promptsView.findViewById(R.id.etUserInput);
                                tvNotes.setText(userInput.getText().toString().trim());
                                app.booksToReadDb.child(book.getBookId()).child("notes").setValue(userInput.getText().toString().trim());
                            }
                        })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                // Create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });

        btnAddNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater li = LayoutInflater.from(BookInfoActivity.this);
                View promptsView = li.inflate(R.layout.alert_dialog, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(BookInfoActivity.this);
                // Set layout alert_dialog.xml to AlertDialog
                alertDialogBuilder.setView(promptsView);

                final EditText userInput = promptsView.findViewById(R.id.etUserInput);
                userInput.setText(book.getNotes().equals("0") ? "" : tvNotes.getText().toString());
                userInput.requestFocus();

                // Set dialog messages
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Get user input and set it to tvNotes
                                tvNotes.setText(userInput.getText().toString());
                                app.booksToReadDb.child(book.getBookId()).child("notes").setValue(userInput.getText().toString().trim());
                            }
                        })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                // Create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });
    }
}