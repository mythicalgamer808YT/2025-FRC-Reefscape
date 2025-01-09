package frc.robot.subsystems;

import java.util.List;

import org.photonvision.PhotonCamera;
import org.photonvision.PhotonUtils;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;

import edu.wpi.first.wpilibj2.command.SubsystemBase;

import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Constants;
import frc.robot.States.PhotonStates;
import frc.robot.generated.TunerConstants;

public class PhotonSubsystem extends SubsystemBase{
    private PhotonCamera camera;
    private List<PhotonPipelineResult> results;
    private PhotonPipelineResult pipelineResult;
    private PhotonStates state;

    private double targetRange;
    private double targetYaw;
    private boolean targetSeen;

    private double cameraHeight;
    private double cameraPitch;

    // Kalman filter variables
    private double estimatedRange;
    private double estimatedYaw;
    private double rangeErrorCovariance = 1;
    private double yawErrorCovariance = 1;
    private final double processNoise = 1e-4; // Adjusted process noise
    private final double measurementNoise = 1;


    public PhotonSubsystem(String cameraName, double cameraHeight, double cameraPitch, PhotonStates state) {
        camera = new PhotonCamera(cameraName);
        this.cameraHeight = cameraHeight;
        this.cameraPitch = cameraPitch;
        this.state = state;
        setState(state);
    }

    @Override 
    public void periodic(){
        results = camera.getAllUnreadResults();
        targetSeen = false;
    
        // check for results and place default values if there are none 
        if(results.isEmpty()) {
            targetYaw = 0;
            targetRange = 0;
            return;
        } 

        // get most recent result
        pipelineResult = results.get(results.size() - 1);
        if(pipelineResult.hasTargets()) {
            for(PhotonTrackedTarget target: pipelineResult.getTargets()) {
                // search for target id 
                if(target.getFiducialId() != state.id)
                {
                    break;
                }
                
                // calculate key data 
                targetSeen = true;
                targetYaw = target.getYaw();
                targetRange = PhotonUtils.calculateDistanceToTargetMeters(
                        cameraHeight, 
                        state.height, 
                        cameraPitch, 
                        Units.degreesToRadians(target.getPitch()));

                targetRange = applyKalmanFilter(targetRange, estimatedRange, rangeErrorCovariance);
            }
        }
        setSmartdashboardData();
    }   

    // set photon subsystem state allowing for change of target and goal 
    public void setState(PhotonStates state) {
        this.state = state;
    }

    public double getRange() {
        return targetRange;
    }

    public double getYaw() {
        return targetYaw;
    }

    // calculate the forward output
    public double getForwardOutput() {
        if(targetSeen) {
            return -(state.distance - targetRange) * TunerConstants.kSpeedAt12Volts.magnitude() * Constants.Photon.driveConstant;
        }
        return 0;
    }

    // calculate the turn output 
    public double getTurnOutput() {
        if(targetSeen) {
           return (state.angle - targetYaw) * Constants.Photon.angleConstant * TunerConstants.kSpeedAt12Volts.magnitude();
        }
        return 0;
    }

    private void setSmartdashboardData() {
        // return motor output data 
        SmartDashboard.putNumber(camera.getName() + " turn motor output", getTurnOutput());
        SmartDashboard.putNumber(camera.getName() + " drive motor output", getForwardOutput());
        
        // return state data 
        SmartDashboard.putNumber(camera.getName() + " current ID", state.id);
        SmartDashboard.putNumber(camera.getName() + " target height", state.height);
        SmartDashboard.putNumber(camera.getName() + " desired distance", state.distance);
        SmartDashboard.putNumber(camera.getName() + " desired angle", state.angle);

        // return calculated tag(target) data
        SmartDashboard.putNumber(camera.getName() + " target yaw", targetYaw);
        SmartDashboard.putNumber(camera.getName() + " target range", targetRange);
        SmartDashboard.putBoolean(camera.getName() + " target seen", targetSeen);
    
    }
        // Apply Kalman filter to smooth values
        private double applyKalmanFilter(double measurement, double estimatedValue, double errorCovariance) {
            // Prediction update
            double predictedValue = estimatedValue;
            double predictedErrorCovariance = errorCovariance + processNoise;
    
            // Measurement update
            double kalmanGain = predictedErrorCovariance / (predictedErrorCovariance + measurementNoise);
            double updatedValue = predictedValue + kalmanGain * (measurement - predictedValue);
            double updatedErrorCovariance = (1 - kalmanGain) * predictedErrorCovariance;
    
            // Update state
            estimatedValue = updatedValue;
            errorCovariance = updatedErrorCovariance;
    
            return updatedValue;
        }

}