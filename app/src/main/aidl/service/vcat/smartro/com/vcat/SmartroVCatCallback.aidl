// SmartroVCatCallback.aidl
package service.vcat.smartro.com.vcat;

// Declare any non-default types here with import statements

interface SmartroVCatCallback {
    oneway void onServiceEvent(String strEventJSON);
    oneway void onServiceResult(String strEventJSON);
}