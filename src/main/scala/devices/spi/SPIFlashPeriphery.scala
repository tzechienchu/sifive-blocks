// See LICENSE for license details.
package sifive.blocks.devices.spi
import sifive.blocks.devices.gpio.{GPIOPin}

import Chisel._
import config.Field
import diplomacy.{LazyModule,LazyMultiIOModuleImp}
import rocketchip.HasSystemNetworks
import uncore.tilelink2.{TLFragmenter,TLWidthWidget}
import util.HeterogeneousBag

case object PeripherySPIFlashKey extends Field[Seq[SPIFlashParams]]

trait HasPeripherySPIFlash extends HasSystemNetworks {
  val spiFlashParams = p(PeripherySPIFlashKey)  
  val qspis = spiFlashParams map { params =>
    val qspi = LazyModule(new TLSPIFlash(peripheryBusBytes, params))
    qspi.rnode := TLFragmenter(peripheryBusBytes, cacheBlockBytes)(peripheryBus.node)
    qspi.fnode := TLFragmenter(1, cacheBlockBytes)(TLWidthWidget(peripheryBusBytes)(peripheryBus.node))
    intBus.intnode := qspi.intnode
    qspi
  }
}

trait HasPeripherySPIFlashBundle {
  val qspi: HeterogeneousBag[SPIPortIO]

  // It is important for SPIFlash that the syncStages is agreed upon, because
  // internally it needs to realign the input data to the output SCK.
  // Therefore, we rely on the syncStages parameter.
  def SPIFlashToGPIOPins(syncStages: Int = 0, driveStrength: Bool = Bool(false)): Seq[SPIPinsIO] = qspi.map { s =>
    SPIPortToGPIOPins(s, syncStages)
  }
}

trait HasPeripherySPIFlashModuleImp extends LazyMultiIOModuleImp with HasPeripherySPIFlashBundle {
  val outer: HasPeripherySPIFlash
  val qspi = IO(HeterogeneousBag(outer.spiFlashParams.map(new SPIPortIO(_))))

  (qspi zip outer.qspis) foreach { case (io, device) => 
    io <> device.module.io.port
  }
}

