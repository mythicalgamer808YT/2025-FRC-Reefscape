package frc.robot.subsystems;

import com.ctre.phoenix6.hardware.TalonFX;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DutyCycleEncoder;
import edu.wpi.first.wpilibj.motorcontrol.PWMSparkFlex;
import edu.wpi.first.wpilibj.motorcontrol.PWMSparkMax;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

import frc.robot.Constants;
import frc.robot.States.ArmStates;
import frc.robot.States.IndexStates;

public class IntakeArmSubsystem extends SubsystemBase {   
    private final TalonFX ArmMotor;
    private final PWMSparkMax indexingMotor;
    private final DutyCycleEncoder armEncoder; 
    private final PIDController armPID;

    private IndexStates indexState;
    private ArmStates armState;
    private double armPosition;

    public IntakeArmSubsystem() {
        ArmMotor = new TalonFX(Constants.Arm.armMotorID);
        indexingMotor = new PWMSparkMax(Constants.Arm.indexingMotorID);
        armEncoder = new DutyCycleEncoder(Constants.Arm.encoderID);
        armPID = new PIDController(Constants.Arm.kP, Constants.Arm.kI, Constants.Arm.kD);
        setIndexState(indexState);
        setArmState(armState);
    }

    @Override
    public void periodic() {
        armPosition = Units.degreesToRadians(armEncoder.get());
        if(armPosition >= ArmStates.MAX.angle || armPosition <= ArmStates.MIN.angle) {
            ArmMotor.set(0);
        } else {
            //ArmMotor.set(armPID.calculate(armEncoder.get()));
            ArmMotor.set(0);
        }
        setSmartdashboard();
    }

    public void setArmState(ArmStates state) {
        this.armState = state;
    }

    public void setIndexState(IndexStates state) {
        indexingMotor.set(state.speed);
        this.indexState = state;
    }

    private void setSmartdashboard() {
        SmartDashboard.putString("Arm Subsytem index state", indexState.toString());
        SmartDashboard.putString("Arm Subsystem arm state ", armState.toString());
        SmartDashboard.putNumber("Arm Subsytem position", armPosition);
    }
}