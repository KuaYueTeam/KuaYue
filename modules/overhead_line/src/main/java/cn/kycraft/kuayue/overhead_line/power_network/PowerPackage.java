package cn.kycraft.kuayue.overhead_line.power_network;

public class PowerPackage {
    private boolean overload;
    private float voltage,current;

    public PowerPackage(float voltage,float current){
        this.voltage = voltage;
        this.current = current;
        overload = false;
    }

    public static final PowerPackage EMPTY =new PowerPackage(0,0);

    public float  getPower(){return current * voltage;}

    public void setCurrent(float current) {this.current = current;}

    public void setVoltage(float voltage){this.voltage = voltage;}

    public float getVoltage(){return voltage;}

    public float getCurrent(){return current;}

    public boolean isOverload() {
        if (overload){return true;}
        overload = current < 0 || voltage < 0;
        return overload;
    }
}
