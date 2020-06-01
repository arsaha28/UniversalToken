package com.universaltoken.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.universaltoken.contracts.UniversalTokenContract;
import com.universaltoken.states.UniversalToken;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@InitiatingFlow
@StartableByRPC
public class UniversalTokenTransferInitiatorFlow extends FlowLogic<SignedTransaction> {
    private final Party issuer;
    private final int amount;
    private final Party receiver;

    public UniversalTokenTransferInitiatorFlow(Party issuer, int amount, Party receiver) {
        this.issuer = issuer;
        this.amount = amount;
        this.receiver = receiver;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        List<StateAndRef<UniversalToken>> allTokenStateAndRefs =
                getServiceHub().getVaultService().queryBy(UniversalToken.class).getStates();

        AtomicInteger totalTokenAvailable = new AtomicInteger();
        List<StateAndRef<UniversalToken>> inputStateAndRef = new ArrayList<>();
        AtomicInteger change = new AtomicInteger(0);

        List<StateAndRef<UniversalToken>> tokenStateAndRefs =  allTokenStateAndRefs.stream()
                .filter(tokenStateStateAndRef -> {
                    if(tokenStateStateAndRef.getState().getData().getIssuer().equals(issuer)){
                        if(totalTokenAvailable.get() < amount){
                            inputStateAndRef.add(tokenStateStateAndRef);
                        }
                        totalTokenAvailable.set(totalTokenAvailable.get() + tokenStateStateAndRef.getState().getData().getAmount());
                        if(change.get() == 0 && totalTokenAvailable.get() > amount){
                            change.set(totalTokenAvailable.get() - amount);
                        }
                        return true;
                    }
                    return false;
                }).collect(Collectors.toList());
        if(totalTokenAvailable.get() < amount){
            throw new FlowException("Insufficient balance");
        }

        UniversalToken outputState = new UniversalToken(issuer, receiver, amount);
        TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache()
                .getNotaryIdentities().get(0))
                .addOutputState(outputState)
                .addCommand(new UniversalTokenContract.Commands.Transfer(), Arrays.asList(getOurIdentity().getOwningKey()));
        inputStateAndRef.forEach(txBuilder::addInputState);

        txBuilder.verify(getServiceHub());
        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);
        FlowSession issuerSession = initiateFlow(issuer);
        FlowSession receiverSession = initiateFlow(receiver);
        SignedTransaction stx = subFlow(new FinalityFlow(signedTransaction, Arrays.asList(issuerSession,receiverSession)));
        return stx;
    }
}