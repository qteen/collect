package org.odk.collect.android.support;

import android.net.Uri;

import org.odk.collect.android.forms.Form;
import org.odk.collect.android.forms.FormsRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import static java.util.stream.Collectors.toList;

public class InMemFormsRepository implements FormsRepository {

    private final List<Form> forms = new ArrayList<>();

    @Override
    public Uri save(Form form) {
        forms.add(form);
        return null;
    }

    @Override
    public List<Form> getByJrFormIdNotDeleted(String jrFormId) {
        return forms.stream().filter(f -> f.getJrFormId().equals(jrFormId) && !f.isDeleted()).collect(toList());
    }

    @Override
    public List<Form> getAll() {
        return new ArrayList<>(forms); // Avoid anything  mutating the list externally
    }

    @Nullable
    @Override
    public Form get(Long id) {
        return forms.stream().filter(f -> f.getId().equals(id)).findFirst().orElse(null);
    }

    @Nullable
    @Override
    public Form get(String jrFormId, @Nullable String jrVersion) {
        return forms.stream().filter(f -> {
            return f.getJrFormId().equals(jrFormId) && Objects.equals(f.getJrVersion(), jrVersion);
        }).findFirst().orElse(null);
    }

    @Nullable
    @Override
    public Form getByMd5Hash(String hash) {
        return forms.stream().filter(f -> f.getMD5Hash().equals(hash)).findFirst().orElse(null);
    }

    @Nullable
    @Override
    public Form getByPath(String path) {
        return forms.stream().filter(f -> f.getFormFilePath().equals(path)).findFirst().orElse(null);
    }

    @Override
    public void delete(Long id) {
        forms.removeIf(form -> form.getId().equals(id));
    }

    @Override
    public void softDelete(Long id) {
        Form form = forms.stream().filter(f -> f.getId().equals(id)).findFirst().orElse(null);

        if (form != null) {
            forms.remove(form);
            forms.add(new Form.Builder(form)
                    .deleted(true)
                    .build());
        }
    }

    @Override
    public void restore(Long id) {
        Form form = forms.stream().filter(f -> f.getId().equals(id)).findFirst().orElse(null);

        if (form != null) {
            forms.remove(form);
            forms.add(new Form.Builder(form)
                    .deleted(false)
                    .build());
        }
    }

    @Override
    public void deleteFormsByMd5Hash(String md5Hash) {
        forms.removeIf(f -> f.getMD5Hash().equals(md5Hash));
    }
}
