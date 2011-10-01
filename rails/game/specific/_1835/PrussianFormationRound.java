package rails.game.specific._1835;

import java.util.*;

import rails.common.DisplayBuffer;
import rails.common.GuiDef;
import rails.common.LocalText;
import rails.game.*;
import rails.game.action.DiscardTrain;
import rails.game.action.PossibleAction;
import rails.game.special.ExchangeForShare;
import rails.game.special.SpecialPropertyI;
import rails.game.model.Owners;

public class PrussianFormationRound extends StockRound {

    private PublicCompany prussian;
    private PublicCompany m2;
    private Phase phase;

	private boolean startPr;
	private boolean forcedStart;
	private boolean mergePr;
	private boolean forcedMerge;

    private List<Company> foldablePrePrussians;

    private enum Step {
        START,
        MERGE,
        DISCARD_TRAINS
    };

    Step step;

	private static String PR_ID = GameManager_1835.PR_ID;
    private static String M2_ID = GameManager_1835.M2_ID;

    public PrussianFormationRound (GameManager gameManager) {
        super (gameManager);

        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);

    }

 	@Override
	public void start() {

        prussian = companyManager.getPublicCompany(PR_ID);
        phase = getCurrentPhase();
		startPr = !prussian.hasStarted();
        forcedMerge = phase.getName().equals("5");
        forcedStart = phase.getName().equals("4+4") || forcedMerge;
 		mergePr = !prussianIsComplete(gameManager);

        ReportBuffer.add(LocalText.getText("StartFormationRound", PR_ID));
        log.debug("StartPr="+startPr+" forcedStart="+forcedStart
        		+" mergePr="+mergePr+" forcedMerge="+forcedMerge);

        step = startPr ? Step.START : Step.MERGE;

        if (step == Step.START) {
            m2 = companyManager.getPublicCompany(M2_ID);
            setCurrentPlayer(m2.getPresident());
            ((GameManager_1835)gameManager).setPrussianFormationStartingPlayer(currentPlayer);
            if (forcedStart) {
                executeStartPrussian(true);
                step = Step.MERGE;
            }
        }

        if (step == Step.MERGE) {
            startingPlayer
                    = ((GameManager_1835)gameManager).getPrussianFormationStartingPlayer();
            log.debug("Original Prussian starting player was "+startingPlayer.getId());
            setCurrentPlayer(startingPlayer);
            if (forcedMerge) {
                List<SpecialPropertyI> sps;
                setFoldablePrePrussians();
                List<Company> foldables = new ArrayList<Company> ();
                for (PrivateCompany company : gameManager.getAllPrivateCompanies()) {
                    sps = company.getSpecialProperties();
                    if (sps != null && !sps.isEmpty() && sps.get(0) instanceof ExchangeForShare) {
                        foldables.add(company);
                    }
                }
                for (PublicCompany company : gameManager.getAllPublicCompanies()) {
                    if (company.isClosed()) continue;
                    sps = company.getSpecialProperties();
                    if (sps != null && !sps.isEmpty() && sps.get(0) instanceof ExchangeForShare) {
                        foldables.add(company);
                    }
                }
                executeExchange (foldables, false, true);
                finishRound();
            } else {
                findNextMergingPlayer(false);
            }
        }
    }

    @Override
	public boolean setPossibleActions() {

        if (step == Step.START) {
            Player m2Owner = m2.getPresident();
            startingPlayer = m2Owner;
            setCurrentPlayer(m2Owner);
            ReportBuffer.add(LocalText.getText("StartingPlayer",
                    getCurrentPlayer().getId()));

            possibleActions.add(new FoldIntoPrussian(m2));

        } else if (step == Step.MERGE) {

            possibleActions.add(new FoldIntoPrussian(foldablePrePrussians));

        } else if (step == Step.DISCARD_TRAINS) {

            if (prussian.getNumberOfTrains() > prussian.getCurrentTrainLimit()) {
                possibleActions.add(new DiscardTrain(prussian,
                        prussian.getPortfolio().getUniqueTrains(), true));
            }
        }
        return true;

    }

    private void setFoldablePrePrussians () {

        foldablePrePrussians = new ArrayList<Company> ();
        SpecialPropertyI sp;
        for (PrivateCompany company : currentPlayer.getPortfolio().getPrivateCompanies()) {
            sp = company.getSpecialProperties().get(0);
            if (sp instanceof ExchangeForShare) {
                foldablePrePrussians.add(company);
            }
        }
        PublicCompany company;
        List<SpecialPropertyI> sps;
        for (PublicCertificate cert : currentPlayer.getPortfolio().getCertificates()) {
            if (!cert.isPresidentShare()) continue;
            company = cert.getCompany();
            sps = company.getSpecialProperties();
            if (sps != null && !sps.isEmpty() && sps.get(0) instanceof ExchangeForShare) {
                foldablePrePrussians.add(company);
            }
        }
    }

    @Override
	protected boolean processGameSpecificAction(PossibleAction action) {

        if (action instanceof FoldIntoPrussian) {

            FoldIntoPrussian a = (FoldIntoPrussian) action;

            if (step == Step.START) {
                if (!startPrussian(a)) {
                    finishRound();
                } else {
                    step = Step.MERGE;
                    findNextMergingPlayer(false);
                }

            } else if (step == Step.MERGE) {

                mergeIntoPrussian (a);

            }

            return true;

        } else if (action instanceof DiscardTrain) {

            discardTrain ((DiscardTrain) action);
            return true;

        } else {
            return false;
        }
    }

    protected boolean findNextMergingPlayer(boolean skipCurrentPlayer) {

        while (true) {

            if (skipCurrentPlayer) {
                setNextPlayer();
                if (getCurrentPlayer() == startingPlayer) {
                    if (prussian.getNumberOfTrains() > prussian.getCurrentTrainLimit()) {
                        step = Step.DISCARD_TRAINS;
                    } else {
                        finishRound();
                    }
                    return false;
                }
            }

            setFoldablePrePrussians();
            if (!foldablePrePrussians.isEmpty()) return true;
            skipCurrentPlayer = true;
        }
    }

    private boolean startPrussian (FoldIntoPrussian action) {

        // Validate
        String errMsg = null;

        List<Company> folded = action.getFoldedCompanies();
        boolean folding = folded != null && !folded.isEmpty();

        while (folding) {
            if (!(M2_ID.equals(action.getFoldedCompanyNames()))) {
                errMsg = LocalText.getText("WrongCompany",
                        action.getFoldedCompanyNames(),
                        GameManager_1835.M2_ID);
                break;
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotMerge",
                    action.getFoldedCompanyNames(),
                    PR_ID,
                    errMsg));
            return false;
        }

        // all actions linked during formation round to avoid serious undo problems
        changeStack.start(false);
        changeStack.linkToPreviousMoveSet();

        if (folding) executeStartPrussian(false);

        return folding;
    }

    private void executeStartPrussian (boolean display) {

        prussian.start();
        String message = LocalText.getText("START_MERGED_COMPANY",
                PR_ID,
                Bank.format(prussian.getIPOPrice()),
                prussian.getStartSpace());
        ReportBuffer.add(message);
        if (display) DisplayBuffer.add(message);

        // add money from sold shares
        // Move cash and shares where required
        int capFactor = getSoldPercentage(prussian) / (prussian.getShareUnit() * prussian.getShareUnitsForSharePrice());
        int cash = capFactor * prussian.getIPOPrice();

        if (cash > 0) {
            Owners.cashMove(bank, prussian, cash);
            ReportBuffer.add(LocalText.getText("FloatsWithCash",
                prussian.getId(),
                Bank.format(cash) ));
        } else {
            ReportBuffer.add(LocalText.getText("Floats",
                    prussian.getId()));
        }
        
        executeExchange (Arrays.asList(new Company[]{m2}), true, false);
        prussian.setFloated();
    }

    private boolean mergeIntoPrussian (FoldIntoPrussian action) {

        // Validate
        // String errMsg = null;

        List<Company> folded = action.getFoldedCompanies();
        boolean folding = folded != null && !folded.isEmpty();

        while (folding) {
            // TODO Some validation needed
            break;
        }

        // TODO: This is now dead code, but won't be when some sensible validations exist 
        /*
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotMerge",
                    action.getFoldedCompanyNames(),
                    PR_ID,
                    errMsg));
            return false; 
        }
        */

        // all actions linked during formation round to avoid serious undo problems
        changeStack.start(false);
        changeStack.linkToPreviousMoveSet();

        // Execute
        if (folding) executeExchange (folded, false, false);

        findNextMergingPlayer(true);

        return folding;
    }

    private void executeExchange (List<Company> companies, boolean president,
         boolean display) {

        ExchangeForShare efs;
        PublicCertificate cert;
        Player player;
        for (Company company : companies) {
            log.debug("Merging company "+company.getId());
            if (company instanceof PrivateCompany) {
                player = (Player)((PrivateCompany)company).getPortfolio().getOwner();
            } else {
                player = ((PublicCompany)company).getPresident();
            }
            // Shortcut, sp should be checked
            efs = (ExchangeForShare) company.getSpecialProperties().get(0);
            cert = unavailable.findCertificate(prussian, efs.getShare()/prussian.getShareUnit(),
            		president);
            cert.moveTo(player.getPortfolio());
            //company.setClosed();
            String message = LocalText.getText("MERGE_MINOR_LOG",
                    player.getId(),
                    company.getId(),
                    PR_ID,
                    company instanceof PrivateCompany ? "no"
                            : Bank.format(((PublicCompany)company).getCash()),
                    company instanceof PrivateCompany ? "no"
                            : ((PublicCompany)company).getPortfolio().getTrainList().size());
            ReportBuffer.add(message);
            if (display) DisplayBuffer.add (message);
            message = LocalText.getText("GetShareForMinor",
                    player.getId(),
                    cert.getShare(),
                    PR_ID,
                    ipo.getId(),
                    company.getId());
            ReportBuffer.add(message);
            if (display) DisplayBuffer.add (message);

            if (company instanceof PublicCompany) {

                PublicCompany minor = (PublicCompany) company;

                // Replace the home token
                BaseToken token = (BaseToken) minor.getTokens().get(0);
                Stop city = (Stop) token.getOwner();
                MapHex hex = city.getHolder();
                token.moveTo(minor);
                if (!hex.hasTokenOfCompany(prussian) && hex.layBaseToken(prussian, city.getNumber())) {
                    /* TODO: the false return value must be impossible. */
                    message = LocalText.getText("ExchangesBaseToken",
                            PR_ID, minor.getId(),
                            city.getId());
                            ReportBuffer.add(message);
                            if (display) DisplayBuffer.add (message);

                    prussian.layBaseToken(hex, 0);
                }

                // Move any cash
                if (minor.getCash() > 0) {
                    Owners.cashMove (minor, prussian, minor.getCash());
                }

                // Move any trains
                // TODO: Simplify code due to trainlist being immutable anyway
                List<Train> trains = new ArrayList<Train> (minor.getPortfolio().getTrainList());
                for (Train train : trains) {
                    train.moveTo(prussian.getPortfolio());
                }
            }

            // Close the merged companies
            company.setClosed();
        }

    }

    public boolean discardTrain(DiscardTrain action) {

        Train train = action.getDiscardedTrain();
        PublicCompany company = action.getCompany();

        String errMsg = null;

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be correct step
            if (company != prussian) {
                errMsg = LocalText.getText("WrongCompany", company.getId(), prussian.getId());
                break;
            }

            if (train == null && action.isForced()) {
                errMsg = LocalText.getText("NoTrainSpecified");
                break;
            }

            // Does the company own such a train?
            if (!company.getPortfolio().getTrainList().contains(train)) {
                errMsg =
                        LocalText.getText("CompanyDoesNotOwnTrain",
                                company.getId(),
                                train.getId() );
                break;
            }

            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotDiscardTrain",
                    company.getId(),
                    (train != null ?train.getId() : "?"),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        changeStack.start(true);
        //
        if (action.isForced()) changeStack.linkToPreviousMoveSet();

        train.moveTo(pool);
        ReportBuffer.add(LocalText.getText("CompanyDiscardsTrain",
                company.getId(),
                train.getId() ));

        // This always finished this type of round
        finishRound();

        return true;
    }

    @Override
    protected void finishRound() {
        RoundI interruptedRound = gameManager.getInterruptedRound();
        ReportBuffer.add(" ");
        if (interruptedRound != null) {  
            ReportBuffer.add(LocalText.getText("EndOfFormationRound", PR_ID, 
                interruptedRound.getRoundName()));
        } else {
            ReportBuffer.add(LocalText.getText("EndOfFormationRoundNoInterrupt", PR_ID));
        }

        if (prussian.hasStarted()) prussian.checkPresidency();
        prussian.setOperated(); // To allow immediate share selling
        //        super.finishRound();
        // Inform GameManager
        gameManager.nextRound(this);
    }

    public static boolean prussianIsComplete(GameManager gameManager) {

        for (PublicCompany company : gameManager.getAllPublicCompanies()) {
            if (!company.getTypeName().equalsIgnoreCase("Minor")) continue;
            if (!company.isClosed()) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "1835 PrussianFormationRound";
    }

}
