package community.growtechsol.com.models;

public class Popularity {
    private int userUpVotes, userDownVotes;

    public int getUserUpVotes() {
        return userUpVotes;
    }

    public void setUserUpVotes(int userUpVotes) {
        this.userUpVotes = userUpVotes;
    }

    public int getUserDownVotes() {
        return userDownVotes;
    }

    public void setUserDownVotes(int userDownVotes) {
        this.userDownVotes = userDownVotes;
    }
}
