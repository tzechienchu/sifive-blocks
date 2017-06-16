// See LICENSE for license details.
package sifive.blocks.devices.spi
import sifive.blocks.devices.gpio.{GPIOPin}

import Chisel._
import config.Field
import diplomacy.{LazyModule,LazyMultiIOModuleImp}
import rocketchip.HasSystemNetworks
import uncore.tilelink2.{TLFragmenter,TLWidthWidget}
import util.HeterogeneousBag

case object PeripherySPIKey extends Field[Seq[SPIParams]]

trait HasPeripherySPI extends HasSystemNetworks {
  val spiParams = p(PeripherySPIKey)  
  val spis = spiParams map { params =>
    val spi = LazyModule(new TLSPI(peripheryBusBytes, params))
    spi.rnode := TLFragmenter(peripheryBusBytes, cacheBlockBytes)(peripheryBus.node)
    intBus.intnode := spi.intnode
    spi
  }
}

trait HasPeripherySPIBundle {
  val spi: HeterogeneousBag[SPIPortIO]

  def SPItoGPIOPins(syncStages: Int = 0, driveStrength: Bool = Bool(false)): Seq[SPIPinsIO] = spi.map { s =>
    SPIPortToGPIOPins(s, syncStages, driveStrength)
  }

}

trait HasPeripherySPIModuleImp extends LazyMultiIOModuleImp with HasPeripherySPIBundle {
  val outer: HasPeripherySPI
  val spi = IO(HeterogeneousBag(outer.spiParams.map(new SPIPortIO(_))))

  (spi zip outer.spis).foreach { case (io, device) =>
    io <> device.module.io.port
  }
}

// This is for the Platform Level.
trait HasSPIGPIOPinsBundle {
  val spi: HeterogeneousBag[SPIPinsIO]
}



