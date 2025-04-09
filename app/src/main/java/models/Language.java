package models;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Language {
    private String uid;
    private String language;
    private String skillLevel;

    public Language() {}

    public Language(String uid, String language, String skillLevel) {
        this.uid = uid;
        this.language = language;
        this.skillLevel = skillLevel;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
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
                ", language='" + language + '\'' +
                ", skillLevel='" + skillLevel + '\'' +
                '}';
    }
}
