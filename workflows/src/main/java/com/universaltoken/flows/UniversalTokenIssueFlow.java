package com.universaltoken.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.universaltoken.contracts.UniversalTokenContract;
import com.universaltoken.states.UniversalToken;
import net.corda.core.contracts.CommandData;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import static java.util.Collections.singletonList;

@StartableByRPC
@InitiatingFlow
public class UniversalTokenIssueFlow extends FlowLogic<String> {

    private final Party owner;
    private final int amount;

    public UniversalTokenIssueFlow(Party owner, int amount) {
        this.owner = owner;
        this.amount = amount;
    }

    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public String call() throws FlowException {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        Party issuer = getOurIdentity();
        final UniversalToken tokenState = new UniversalToken(issuer,owner,amount);
        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);
        CommandData commandData = new UniversalTokenContract.Commands.Issue();
        transactionBuilder.addCommand(commandData, issuer.getOwningKey(), owner.getOwningKey());
        transactionBuilder.addOutputState(tokenState, UniversalTokenContract.ID);
        transactionBuilder.verify(getServiceHub());
        FlowSession session = initiateFlow(owner);
        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);
        SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(signedTransaction, singletonList(session)));
        SignedTransaction stx = subFlow(new FinalityFlow(fullySignedTransaction, singletonList(session)));
        return "\nTransaction ID: "+stx.getId();
    }
}
