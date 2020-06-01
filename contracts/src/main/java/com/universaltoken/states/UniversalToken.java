package com.universaltoken.states;

import com.universaltoken.contracts.UniversalTokenContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(UniversalTokenContract.class)
public class UniversalToken implements ContractState {

    private final Party owner;
    private final int amount;
    private final Party issuer;

    public UniversalToken(Party issuer, Party owner, int amount) {
        this.issuer = issuer;
        this.owner = owner;
        this.amount = amount;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(issuer, owner);
    }
    public Party getIssuer() {
        return issuer;
    }
    public Party getOwner() {
        return owner;
    }
    public int getAmount() {
        return amount;
    }
}
