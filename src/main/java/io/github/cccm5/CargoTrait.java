package io.github.cccm5;

import com.degitise.minevid.dtlTraders.utils.citizens.TraderTrait;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
/**
* CargoTrait is a marker trait that needs to be added to all dtl traders that can interact with Movecraft crafts
*/
@TraitName("cargo")
public class CargoTrait extends Trait
{

    public CargoTrait()
    {
        super("cargo");
    }

    @Override
    public void onAttach(){
        if(!this.npc.hasTrait(TraderTrait.class))
            this.npc.removeTrait(CargoTrait.class);
    }

}
