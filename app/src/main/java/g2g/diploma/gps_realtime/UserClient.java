package g2g.diploma.gps_realtime;

import android.app.Application;

import g2g.diploma.gps_realtime.UserUsage.User;

public class UserClient extends Application {

    private User user = null;

    public User getUser() {return user;}

    public void setUser(User user) {this.user = user;}
}
