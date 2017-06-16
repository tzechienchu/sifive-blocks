// See LICENSE for license details.
package sifive.blocks.devices.spi

import Chisel._
import chisel3.experimental.{Analog}
import sifive.blocks.devices.gpio.{GPIOPin, GPIOOutputPinCtrl, GPIOInputPinCtrl}

class SPIPinsIO(c: SPIParamsBase) extends SPIBundle(c) {
  val sck = new GPIOPin()
  val dq = Vec(4, new GPIOPin())
  val cs = Vec(c.csWidth, new GPIOPin())

}

object SPIPortToGPIOPins { 

  def apply(spi: SPIPortIO, syncStages: Int = 0, driveStrength: Bool = Bool(false)) : SPIPinsIO = {

    val pins = Wire(new SPIPinsIO(spi.c))
    GPIOOutputPinCtrl(pins.sck, spi.sck, ds = driveStrength)

    GPIOOutputPinCtrl(pins.dq, Bits(0, spi.dq.size))
      (pins.dq zip spi.dq).foreach {
        case (p, s) =>
          p.o.oval := s.o
          p.o.oe  := s.oe
          p.o.ie  := ~s.oe
          p.o.pue := Bool(true)
          p.o.ds  := driveStrength
          s.i := ShiftRegister(p.i.ival, syncStages)
      }

    GPIOOutputPinCtrl(pins.cs, spi.cs.asUInt)
    pins.cs.foreach(_.o.ds := driveStrength)
    pins
  }
}

class SPIPadsIO(c: SPIParamsBase) extends SPIBundle(c) {
  val sck = Analog(1.W)
  val dq = Vec(4, Analog(1.W))
  val cs = Vec(c.csWidth, Analog(1.W))
}
