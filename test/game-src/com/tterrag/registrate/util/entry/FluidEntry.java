package com.tterrag.registrate.util.entry;
import com.tterrag.registrate.fabric.SimpleFlowableFluid;
public class FluidEntry {
	private final SimpleFlowableFluid flowing, source;
	public FluidEntry(String id) { flowing = new SimpleFlowableFluid(id + "_flowing"); source = new SimpleFlowableFluid(id); }
	public Object get() { return flowing; }
	public SimpleFlowableFluid getSource() { return source; }
}
