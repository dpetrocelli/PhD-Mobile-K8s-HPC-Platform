package BoostrapRat;

public class Job {
    int id;
    String user;
    String path;
    String params;
    String state;
    int totalparts;
    int partscompleted;
    String cloudStorage;

    public Job() {
    }
   
    public Job(int id, String user, String path, String params, String state, int totalparts, int partscompleted, String cloudStorage) {
        this.id = id;
        this.user = user;
        this.path = path;
        this.params = params;
        this.state = state;
        this.totalparts = totalparts;
        this.partscompleted = partscompleted;
        this.cloudStorage = cloudStorage;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCloudStorage() {
        return this.cloudStorage;
    }

    public void setCloudStorage(String cloudStorage) {
        this.cloudStorage = cloudStorage;
    }
    public String getUser() {
        return this.user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getParams() {
        return this.params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getState() {
        return this.state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getTotalparts() {
        return this.totalparts;
    }

    public void setTotalparts(int totalparts) {
        this.totalparts = totalparts;
    }

    public int getPartscompleted() {
        return this.partscompleted;
    }

    public void setPartscompleted(int partscompleted) {
        this.partscompleted = partscompleted;
    }

    public Job id(int id) {
        this.id = id;
        return this;
    }

    public Job user(String user) {
        this.user = user;
        return this;
    }

    public Job path(String path) {
        this.path = path;
        return this;
    }

    public Job params(String params) {
        this.params = params;
        return this;
    }

    public Job state(String state) {
        this.state = state;
        return this;
    }

    public Job totalparts(int totalparts) {
        this.totalparts = totalparts;
        return this;
    }

    public Job partscompleted(int partscompleted) {
        this.partscompleted = partscompleted;
        return this;
    }

    
    @Override
    public String toString() {
        return "{" +
            " id='" + getId() + "'" +
            ", user='" + getUser() + "'" +
            ", path='" + getPath() + "'" +
            ", params='" + getParams() + "'" +
            ", state='" + getState() + "'" +
            ", totalparts='" + getTotalparts() + "'" +
            ", partscompleted='" + getPartscompleted() + "'" +
            "}";
    }

}
