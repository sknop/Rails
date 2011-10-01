package rails.game.specific._1835;

import java.util.*;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.common.parser.GameOption;
import rails.game.*;
import rails.game.action.DiscardTrain;
import rails.game.action.LayTile;
import rails.game.model.CashOwner;
import rails.game.model.HolderModel;
import rails.game.model.Owner;
import rails.game.special.*;
import rails.game.state.BooleanState;
import rails.game.state.HashMapState;
import rails.game.model.Owners;

public class OperatingRound_1835 extends OperatingRound {

    private BooleanState needPrussianFormationCall
            = new BooleanState (this, "NeedPrussianFormationCall", false);
    private BooleanState hasLaidExtraOBBTile
            = new BooleanState (this, "HasLaidExtraOBBTile", false);

    /**
     * Registry of percentage of PR revenue to be denied per player
     * because of having produced revenue in the same OR.
     */
    private HashMapState<Player, Integer> deniedIncomeShare;

    public OperatingRound_1835 (GameManager gameManager) {
        super (gameManager);
        deniedIncomeShare = new HashMapState<Player, Integer> (this, "deniedIncomeShare");
    }

    /** Can a public company operate? (1835 special version) */
    @Override
    protected boolean canCompanyOperateThisRound (PublicCompany company) {
        if (!company.hasFloated() || company.isClosed()) {
            return false;
        }
        // 1835 specials
        // Majors always operate
        if (company.hasStockPrice()) return true;
        // In some variants minors don't run if BY has not floated
        if (gameManager.getGameOption(GameOption.VARIANT).equalsIgnoreCase("Clemens")
                || gameManager.getGameOption("MinorsRequireFloatedBY").equalsIgnoreCase("yes")) {
            return companyManager.getPublicCompany(GameManager_1835.BY_ID).hasFloated();
        }
        return true;
    }

    @Override
    protected void privatesPayOut() {
        int count = 0;
        for (PrivateCompany priv : companyManager.getAllPrivateCompanies()) {
            if (!priv.isClosed()) {
                // The bank portfolios are all not cashOwners themselves!
                if (priv.getOwner() instanceof CashOwner) {
                    Owner recipient = priv.getOwner();
                    int revenue = priv.getRevenueByPhase(getCurrentPhase()); // sfy 1889: revenue by phase
                    if (count++ == 0) ReportBuffer.add("");
                    ReportBuffer.add(LocalText.getText("ReceivesFor",
                            recipient.getId(),
                            Bank.format(revenue),
                            priv.getId()));
                    Owners.cashMove(bank, (CashOwner)recipient, revenue);

                    /* Register black private equivalent PR share value
                     * so it can be subtracted if PR operates */
                    if (recipient instanceof Player && priv.getSpecialProperties() != null
                            && priv.getSpecialProperties().size() > 0) {
                        SpecialPropertyI sp = priv.getSpecialProperties().get(0);
                        if (sp instanceof ExchangeForShare) {
                            ExchangeForShare efs = (ExchangeForShare) sp;
                            if (efs.getPublicCompanyName().equalsIgnoreCase(GameManager_1835.PR_ID)) {
                                int share = efs.getShare();
                                Player player = (Player) recipient;
                                addIncomeDenialShare (player, share);
                            }

                        }
                    }
                }

            }
        }

    }

    private void addIncomeDenialShare (Player player, int share) {

        if (!deniedIncomeShare.containsKey(player)) {
            deniedIncomeShare.put(player, share);
        } else {
            deniedIncomeShare.put(player, share + deniedIncomeShare.get(player));
        }
        //log.debug("+++ Denied "+share+"% share of PR income to "+player.getName());
    }

    /** Count the number of shares per revenue recipient<p>
     * A special rule applies to 1835 to prevent black privates and minors providing
     * income twice during an OR.
     */
    @Override
    protected  Map<CashOwner, Integer>  countSharesPerRecipient () {

        Map<CashOwner, Integer> sharesPerRecipient = super.countSharesPerRecipient();

        if (operatingCompany.get().getId().equalsIgnoreCase(GameManager_1835.PR_ID)) {
            for (Player player : deniedIncomeShare.viewKeySet()) {
                if (!sharesPerRecipient.containsKey(player)) continue;
                int share = deniedIncomeShare.get(player);
                int shares = share / operatingCompany.get().getShareUnit();
                sharesPerRecipient.put (player, sharesPerRecipient.get(player) - shares);
                ReportBuffer.add(LocalText.getText("NoIncomeForPreviousOperation",
                        player.getId(),
                        share,
                        GameManager_1835.PR_ID));

            }
        }

        return sharesPerRecipient;
    }

     /**
      * Register black minors as having operated
     * for the purpose of denying income after conversion to a PR share
     */
    @Override
    protected void initTurn() {

        super.initTurn();

        List<SpecialPropertyI> sps = operatingCompany.get().getSpecialProperties();
        if (sps != null && !sps.isEmpty()) {
            ExchangeForShare efs = (ExchangeForShare) sps.get(0);
            addIncomeDenialShare (operatingCompany.get().getPresident(), efs.getShare());
        }
    }

    @Override
    public void resume() {
        PublicCompany prussian = companyManager.getPublicCompany(GameManager_1835.PR_ID);

        if (prussian.hasFloated() && !prussian.hasOperated()
                // PR has just started. Check if it can operate this round
                // That's only the case if M1 has just bought
                // the first 4-train or 4+4-train
                && operatingCompany.get() == companyManager.getPublicCompany("M1")) {
            log.debug("M2 has not operated: PR can operate");

            // Insert the Prussian before the first major company
            // with a lower current price that has not yet operated
            // and isn't currently operating

            int index = 0;
            int operatingCompanyndex = getOperatingCompanyndex();
            for (PublicCompany company : setOperatingCompanies()) {
                if (index > operatingCompanyndex
                        && company.hasStockPrice()
                        && company.hasFloated()
                        && !company.isClosed()
                        && company != operatingCompany.get()
                        && company.getCurrentSpace().getPrice()
                            < prussian.getCurrentSpace().getPrice()) {
                    log.debug("PR will operate before "+company.getId());
                    break;
                }
                index++;
            }
            // Insert PR at the found index (possibly at the end)
            operatingCompanies.add(index, prussian);
            log.debug("PR will operate at order position "+index);

        } else {

            log.debug("M2 has operated: PR cannot operate");

        }

        guiHints.setCurrentRoundType(getClass());
        super.resume();
    }

    @Override
    protected List<LayTile> getSpecialTileLays(boolean display) {

        /* Special-property tile lays */
        List<LayTile> currentSpecialTileLays = new ArrayList<LayTile>();

        if (operatingCompany.get().canUseSpecialProperties()) {

            for (SpecialTileLay stl : getSpecialProperties(SpecialTileLay.class)) {
                if (stl.isExtra() 
                          // If the special tile lay is not extra, it is only allowed if
                          // normal tile lays are also (still) allowed
                      || stl.getTile() != null 
                          && getCurrentPhase().isTileColourAllowed(stl.getTile().getColourName())) {

                    // Exclude the second OBB free tile if the first was laid in this round
                    if (stl.getLocationNameString().matches("M1(7|9)")
                            && hasLaidExtraOBBTile.booleanValue()) continue;

                    currentSpecialTileLays.add(new LayTile(stl));
                }
            }
        }

        if (display) {
            int size = currentSpecialTileLays.size();
            if (size == 0) {
                log.debug("No special tile lays");
            } else {
                for (LayTile tileLay : currentSpecialTileLays) {
                    log.debug("Special tile lay: " + tileLay.toString());
                }
            }
        }
        
        return currentSpecialTileLays;
    }
    @Override
    public boolean layTile(LayTile action) {
        
        boolean hasJustLaidExtraOBBTile = action.getSpecialProperty() != null
                && action.getSpecialProperty().getLocationNameString().matches("M1(5|7)");

        // The extra OBB tiles may not both be laid in the same round
        if (hasJustLaidExtraOBBTile) {
            if (hasLaidExtraOBBTile.booleanValue()) {
                String errMsg = LocalText.getText("InvalidTileLay");
                DisplayBuffer.add(LocalText.getText("CannotLayTileOn",
                        action.getCompanyName(),
                        action.getLaidTile().getExternalId(),
                        action.getChosenHex().getId(),
                        Bank.format(0),
                        errMsg ));
                return false;
            } else {
                changeStack.start(true); // Duplicate, but we have to
                hasLaidExtraOBBTile.set(true); 
                // Done here to make getSpecialTileLays() return the correct value.
                // It's provisional, on the assumption that other validations are OK.
                // TODO To get it really right, we should separate validation and execution.
            }
        }

        boolean result = super.layTile(action);

        if (!result && hasJustLaidExtraOBBTile) {
            // Revert if tile lay is unsuccessful
            hasLaidExtraOBBTile.set(false);
        }

        return result;
    }

    @Override
    protected void newPhaseChecks() {
        Phase phase = getCurrentPhase();
        if (phase.getName().equals("4") 
                || phase.getName().equals("4+4") 
                    && !companyManager.getPublicCompany(GameManager_1835.PR_ID).hasStarted()
                || phase.getName().equals("5")
                    && !PrussianFormationRound.prussianIsComplete(gameManager)) {
            if (getStep() == GameDef.OrStep.DISCARD_TRAINS) {
                // Postpone until trains are discarded
                needPrussianFormationCall.set(true);
            } else {
                // Do it immediately
                ((GameManager_1835)gameManager).startPrussianFormationRound (this);
            }
        }
    }

    @Override
    public boolean discardTrain(DiscardTrain action) {

        boolean result = super.discardTrain(action);
        if (result && getStep() == GameDef.OrStep.BUY_TRAIN
                && needPrussianFormationCall.booleanValue()) {
            // Do the postponed formation calls
            ((GameManager_1835)gameManager).startPrussianFormationRound (this);
            needPrussianFormationCall.set(false);
        }
        return result;
    }
}
