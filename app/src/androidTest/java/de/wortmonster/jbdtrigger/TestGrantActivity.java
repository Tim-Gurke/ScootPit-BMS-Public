package de.wortmonster.jbdtrigger;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
/** Grants trees from the test provider's owning UID, as a document picker does. Test APK only. */
public class TestGrantActivity extends Activity {
    @Override protected void onCreate(Bundle saved){super.onCreate(saved);
        int flags=Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
        for(String root:new String[]{"root","readonly"})grantUriPermission("de.wortmonster.jbdtrigger",Uri.parse("content://de.wortmonster.jbdtrigger.test.documents/tree/"+root),flags);
        finish();
    }
}
