// Copyright © 2019 Barnaby Shearer <b@Zi.iS>
/*
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation version 3.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package is.zi.huewidgets;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.Objects;


public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(getActionBar()).setTitle(R.string.app_title);

        setContentView(R.layout.main);

        findViewById(R.id.add).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ConfigureActivity.class));
            finish();
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        getFragmentManager().findFragmentById(R.id.fragment).onActivityResult(requestCode, resultCode, data);
    }
}
