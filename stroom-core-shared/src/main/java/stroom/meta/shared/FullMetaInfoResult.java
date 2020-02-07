package stroom.meta.shared;



import java.util.List;

public class FullMetaInfoResult {
    private List<Section> sections;

    public FullMetaInfoResult() {
    }

    public FullMetaInfoResult(final List<Section> sections) {
        this.sections = sections;
    }

    public List<Section> getSections() {
        return sections;
    }

    public static class Section {
        private String title;
        private List<Entry> entries;

        public Section() {
        }

        public Section(final String title, final List<Entry> entries) {
            this.title = title;
            this.entries = entries;
        }

        public String getTitle() {
            return title;
        }

        public List<Entry> getEntries() {
            return entries;
        }
    }

    public static class Entry {
        private String key;
        private String value;

        public Entry() {
        }

        public Entry(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }
}
