package de.wortmonster.jbdtrigger;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import java.io.File;
import java.io.FileNotFoundException;

/** A test-only SAF provider: exercises real ContentResolver streams and provider failures. */
public class TestDocumentsProvider extends DocumentsProvider {
    private static final String[] DOCS={"document_id","_display_name","mime_type","flags","_size","last_modified"};
    private File file(String id)throws FileNotFoundException{
        if(id.equals("root"))return getContext().getFilesDir();
        if(!id.startsWith("root/")||id.substring(5).contains("/")||id.contains(".."))throw new FileNotFoundException(id);
        return new File(getContext().getFilesDir(),id.substring(5));
    }
    @Override public boolean isChildDocument(String parent,String child){return parent.equals("root") && child.startsWith("root/");}
    @Override public boolean onCreate(){return true;}
    @Override public Cursor queryRoots(String[] projection){MatrixCursor c=new MatrixCursor(new String[]{"root_id","document_id","title","flags"});c.addRow(new Object[]{"root","root","Test",DocumentsContract.Root.FLAG_SUPPORTS_CREATE});return c;}
    private void row(MatrixCursor c,String id,File f){MatrixCursor.RowBuilder row=c.newRow();for(String column:c.getColumnNames())switch(column){case "document_id":row.add(id);break;case "_display_name":row.add(f.getName());break;case "mime_type":row.add(f.isDirectory()?DocumentsContract.Document.MIME_TYPE_DIR:"application/octet-stream");break;case "flags":row.add(DocumentsContract.Document.FLAG_SUPPORTS_WRITE|DocumentsContract.Document.FLAG_SUPPORTS_DELETE|DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE);break;case "_size":row.add(f.length());break;case "last_modified":row.add(f.lastModified());break;default:row.add(null);}}
    @Override public Cursor queryDocument(String id,String[] projection)throws FileNotFoundException{MatrixCursor c=new MatrixCursor(projection==null?DOCS:projection);row(c,id,file(id));return c;}
    @Override public Cursor queryChildDocuments(String parent,String[] projection,String order)throws FileNotFoundException{MatrixCursor c=new MatrixCursor(projection==null?DOCS:projection);if(parent.equals("readonly"))return c;File[] files=file(parent).listFiles();if(files!=null)for(File f:files)row(c,"root/"+f.getName(),f);return c;}
    @Override public String createDocument(String parent,String mime,String name)throws FileNotFoundException{if(!parent.equals("root"))throw new FileNotFoundException("read-only test folder");File f=file("root/"+name);try{f.createNewFile();}catch(Exception e){throw new FileNotFoundException(e.getMessage());}return "root/"+name;}
    @Override public void deleteDocument(String id)throws FileNotFoundException{if(!file(id).delete())throw new FileNotFoundException(id);}
    @Override public ParcelFileDescriptor openDocument(String id,String mode,CancellationSignal signal)throws FileNotFoundException{return ParcelFileDescriptor.open(file(id),ParcelFileDescriptor.parseMode(mode));}
}
