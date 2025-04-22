package models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.List;

@IgnoreExtraProperties
public class Language {
    private String uid;
    private List<String> languages;
    private String skillLevel;

    public Language() {}

    public Language(String uid, List<String> languages, String skillLevel) {
        this.uid = uid;
        this.languages = languages;
        this.skillLevel = skillLevel;
    }

    public String getUid() {
        return uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }

    public List<String> getLanguages() {
        return languages;
    }
    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public String getSkillLevel() {
        return skillLevel;
    }
    public void setSkillLevel(String skillLevel) {
        this.skillLevel = skillLevel;
    }

    @Override
    public String toString() {
        return "Language{" +
                "uid='" + uid + '\'' +
                ", languages=" + languages +
                ", skillLevel='" + skillLevel + '\'' +
                '}';
    }
}
