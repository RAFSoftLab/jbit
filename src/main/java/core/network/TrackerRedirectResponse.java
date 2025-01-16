package core.network;

public class TrackerRedirectResponse extends TrackerNetworkResponse{

    private final String redirectUrl;

    public TrackerRedirectResponse(String redirectUrl){
        this.redirectUrl = redirectUrl;
    }

    public String getRedirectUrl(){
        return redirectUrl;
    }

    @Override
    public String toString(){
        return "Redirecting to: " + redirectUrl;
    }



}
