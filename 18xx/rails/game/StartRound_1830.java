package rails.game;

import java.util.*;

import rails.game.action.BuyOrBidStartItem;
import rails.game.action.NullAction;
import rails.game.move.MoveSet;
import rails.util.LocalText;


/**
 * Implements an 1830-style initial auction.
 */
public class StartRound_1830 extends StartRound
{

	/**
	 * Constructor, only to be used in dynamic instantiation.
	 */
	public StartRound_1830()
	{
		super();
		hasBidding = true;
	}

	/**
	 * Start the 1830-style start round.
	 * 
	 * @param startPacket
	 *            The startpacket to be sold in this start round.
	 */
	public void start(StartPacket startPacket)
	{
		super.start(startPacket);
		
		setPossibleActions();
		
	}

	public boolean setPossibleActions() {
		
		boolean passAllowed = true;
		
		possibleActions.clear();
		
		if (StartPacket.getStartPacket().areAllSold()) return false;
		
		//StartItem item;
		StartItem auctionItem = (StartItem) auctionItemState.getObject();
		
		while (possibleActions.isEmpty()) {
			
			Player currentPlayer = getCurrentPlayer();

			//for (Iterator it = itemsToSell.iterator(); it.hasNext(); ) {
			for (StartItem item : itemsToSell) {
				//item = (StartItem) it.next();
				
				if (item.isSold()) {
					// Don't include
				} else if (item.getStatus() == StartItem.AUCTIONED) {
					item.setStatus (StartItem.AUCTIONED);
					if (currentPlayer.getFreeCash() 
							+ auctionItem.getBid(currentPlayer)
							>= auctionItem.getMinimumBid()) {
						BuyOrBidStartItem possibleAction = new BuyOrBidStartItem (
								auctionItem, 
								auctionItem.getMinimumBid(), 
								StartItem.AUCTIONED);
						possibleActions.add (possibleAction);
						break; // No more actions
					} else {
						// Can't bid: Autopass
						break;
					}
				} else if (item.getStatus() == StartItem.NEEDS_SHARE_PRICE) {
					/* This status is set in buy() if a share price is missing */
					possibleActions.add(new BuyOrBidStartItem(
							item, 0, StartItem.NEEDS_SHARE_PRICE));
					passAllowed = false;
					break; // No more actions
				} else if (item == startPacket.getFirstUnsoldItem()) {
					if (item.getBidders() == 1) {
						// Bid upon by one player.
						// If we need a share price, ask for it.
						PublicCompanyI comp = item.needsPriceSetting();
						if (comp != null) {
							setPlayer (item.getBidder());
							BuyOrBidStartItem newItem = 
								new BuyOrBidStartItem (item, item.getBasePrice(), StartItem.NEEDS_SHARE_PRICE);
							newItem.setActualBid(item.getBid());
							possibleActions.add(newItem);
							break; // No more actions possible!
						} else {
							// Otherwise, buy it now.
							assignItem (item.getBidder(), item, item.getBid(), 0);
						}
					} else if (item.getBidders() > 1) {
						ReportBuffer.add(LocalText.getText("TO_AUCTION", item.getName()));
						// Start left of the currently highest bidder
						if (item.getStatus() != StartItem.AUCTIONED) {
							setNextBiddingPlayer(item, item.getBidder().getIndex());
							currentPlayer = getCurrentPlayer();
							item.setStatus (StartItem.AUCTIONED);
							auctionItemState.set (item);
							//numBidders = item.getBidders();
						}
						if (currentPlayer.getFreeCash() 
								+ item.getBid(currentPlayer)
								>= item.getMinimumBid()) {
							BuyOrBidStartItem possibleAction = new BuyOrBidStartItem (
									item, 
									item.getMinimumBid(), 
									StartItem.AUCTIONED);
							possibleActions.add (possibleAction);
						}
						break; // No more possible actions!
					} else {
						item.setStatus(StartItem.BUYABLE);
						if (currentPlayer.getFreeCash() >= item.getBasePrice()) {
							possibleActions.add (new BuyOrBidStartItem (
									item, 
									item.getBasePrice(), 
									StartItem.BUYABLE));
						}
					}
				} else {
					item.setStatus(StartItem.BIDDABLE);
					if (currentPlayer.getFreeCash() 
							+ item.getBid(currentPlayer)
							>= item.getMinimumBid()) {
						possibleActions.add (new BuyOrBidStartItem (
								item, 
								item.getMinimumBid(), 
								StartItem.BIDDABLE));
					}
				}
	
			}
			
			if (possibleActions.isEmpty()) {
				numPasses.add(1);
				if (auctionItemState.getObject() == null) {
					setNextPlayer();
				} else {
					setNextBiddingPlayer ((StartItem)auctionItemState.getObject());
				}
			}
		}
		
		if (passAllowed) {
			possibleActions.add (new NullAction (NullAction.PASS));
		}
		
		//for (Iterator it = possibleActions.getList().iterator();
		//	it.hasNext(); ) {
		//	log.debug("~Action: "+it.next().toString());
		//}
		//log.debug("numPasses="+numPasses.intValue());
		return true;
	}
	
	/**
	 * Return the start items, marked as appropriate for an 1830-style auction.
	 */
	public List<StartItem> getStartItems () {
		
		StartItem item;
		Player currentPlayer = getCurrentPlayer();
		StartItem auctionItem = (StartItem) auctionItemState.getObject();
		
		for (Iterator it = itemsToSell.iterator(); it.hasNext(); ) {
			item = (StartItem) it.next();
			
			if (item.isSold()) {
				item.setStatus (StartItem.SOLD);
			} else if (auctionItem != null) {
				// There is just one tradeable item: the one up for auctioning
				item.setStatus(item.equals(auctionItem)
						? StartItem.AUCTIONED 
						: StartItem.UNAVAILABLE);
			} else {
				item.setStatus(item == startPacket.getFirstUnsoldItem()
						? StartItem.BUYABLE
						: StartItem.BIDDABLE);
			}

			if (item.getStatus() == StartItem.BUYABLE
					&& currentPlayer.getFreeCash() < item.getBasePrice()) {
				item.setStatus(StartItem.UNAVAILABLE);
			} else if ((item.getStatus() == StartItem.BIDDABLE
					|| item.getStatus() == StartItem.AUCTIONED)
					&& currentPlayer.getFreeCash() + item.getBid(currentPlayer)
						< item.getMinimumBid()) {
				item.setStatus(StartItem.UNAVAILABLE);
			}
		}
		return itemsToSell;
	}


	/*----- MoveSet methods -----*/
	/**
	 * The current player bids on a given start item.
	 * 
	 * @param playerName
	 *            The name of the current player (for checking purposes).
	 * @param itemName
	 *            The name of the start item on which the bid is placed.
	 * @param amount
	 *            The bid amount.
	 */
	protected boolean bid(String playerName, BuyOrBidStartItem bidItem)
	{

		StartItem item = bidItem.getStartItem();
		String errMsg = null;
		Player player = GameManager.getCurrentPlayer();
		int previousBid = 0;
		int bidAmount = bidItem.getActualBid();

		while (true)
		{

			// Check player
			if (!playerName.equals(player.getName()))
			{
				errMsg = LocalText.getText("WrongPlayer", playerName);
				break;
			}
			// Check item
			boolean validItem = false;
			for (Iterator it = possibleActions.getType(BuyOrBidStartItem.class).iterator();
					it.hasNext();) { 
				BuyOrBidStartItem activeItem = (BuyOrBidStartItem) it.next();
				if (bidItem.equals(activeItem)) {
					validItem = true;
					break;
				}
				
			}
			if (!validItem)
			{
				errMsg = LocalText.getText("ActionNotAllowed", bidItem.toString());
				break;
			}

			// Is the item buyable?
			if (bidItem.getStatus() != StartItem.BIDDABLE
					&& bidItem.getStatus() != StartItem.AUCTIONED)
			{
				errMsg = LocalText.getText("NotForSale");
				break;
			}
			
			// Bid must be at least 5 above last bid
			if (bidAmount < item.getMinimumBid())
			{
				errMsg = LocalText.getText("BidTooLow", ""+item.getMinimumBid());
				break;
			}
			previousBid = item.getBid(player);
			int available = player.getFreeCash() + previousBid;
			if (bidAmount > available)
			{
				errMsg = LocalText.getText("BidTooHigh", Bank.format(available));
				break;
			}

			break;
		}

		if (errMsg != null)
		{
			DisplayBuffer.add(LocalText.getText("InvalidBid", new String[]{
					playerName,
					item.getName(),
					errMsg
				}));
			return false;
		}

		MoveSet.start(true);
		
		item.setBid(bidAmount, player);
		if (previousBid > 0)
			player.unblockCash(previousBid);
		player.blockCash(bidAmount);
		ReportBuffer.add(LocalText.getText("BID_ITEM_LOG", new String[] {
				playerName,
				Bank.format(bidAmount),
				item.getName(),
				Bank.format(player.getFreeCash())
			}));

		if (bidItem.getStatus() != StartItem.AUCTIONED)
		{
			GameManager.setNextPlayer();
		}
		else
		{
			setNextBiddingPlayer(item);
		}
		numPasses.set(0);

		return true;

	}


	/**
	 * Process a player's pass.
	 * 
	 * @param playerName
	 *            The name of the current player (for checking purposes).
	 */
	protected boolean pass(String playerName)
	{

		String errMsg = null;
		Player player = GameManager.getCurrentPlayer();
		StartItem auctionItem = (StartItem) auctionItemState.getObject();

		while (true)
		{

			// Check player
			if (!playerName.equals(player.getName()))
			{
				errMsg = LocalText.getText("WrongPlayer", playerName);
				break;
			}
			break;
		}

		if (errMsg != null)
		{
			DisplayBuffer.add(LocalText.getText("InvalidPass", new String[] {
					playerName,
					errMsg
				}));
			return false;
		}

		ReportBuffer.add(LocalText.getText("PASSES", playerName));
		
		MoveSet.start(true);
		
		numPasses.add (1);

		if (auctionItem != null)
		{

			if (numPasses.intValue() == auctionItem.getBidders() - 1)
			{
				// All but the highest bidder have passed.
				int price = auctionItem.getBid();

				log.debug("Highest bidder is "+auctionItem.getBidder().getName());
				if (auctionItem.needsPriceSetting() != null) {
					auctionItem.setStatus(StartItem.NEEDS_SHARE_PRICE);
				} else {
					assignItem(auctionItem.getBidder(), auctionItem, price, 0);
				}
				auctionItemState.set(null);
				numPasses.set(0);
			}
			else
			{
				// More than one left: find next bidder
				setNextBiddingPlayer(auctionItem, GameManager.getCurrentPlayerIndex());
			}

		}
		else
		{

			if (numPasses.intValue() >= numPlayers)
			{
				// All players have passed.
				ReportBuffer.add(LocalText.getText("ALL_PASSED"));
				// It the first item has not been sold yet, reduce its price by
				// 5.
				if (startPacket.getFirstUnsoldItem() == startPacket.getFirstItem())
				{
					startPacket.getFirstItem().reduceBasePriceBy(5);
					ReportBuffer.add(LocalText.getText("ITEM_PRICE_REDUCED",
							new String[] {
								startPacket.getFirstItem().getName(),
								Bank.format(startPacket.getFirstItem().getBasePrice())
						}));
					numPasses.set(0);
					if (startPacket.getFirstItem().getBasePrice() == 0)
					{
						assignItem (getCurrentPlayer(),
								startPacket.getFirstItem(), 0, 0);
						GameManager.setPriorityPlayer();
								//startPacket.getFirstItem().getName());
					}
				} else {
					numPasses.set(0);
					GameManager.getInstance().nextRound(this);
					
				}
			} else if (auctionItem != null) {
				setNextBiddingPlayer(auctionItem);
			} else {
				setNextPlayer();
			}
		}

		return true;
	}

	private void setNextBiddingPlayer(StartItem item, int currentIndex)
	{
		for (int i = currentIndex + 1; i < currentIndex
				+ GameManager.getNumberOfPlayers(); i++)
		{
			if (item.hasBid(GameManager.getPlayer(i).getName()))
			{
				GameManager.setCurrentPlayerIndex(i);
				break;
			}
		}
	}
	
	private void setNextBiddingPlayer (StartItem item) {
		
		setNextBiddingPlayer (item, GameManager.getCurrentPlayerIndex());
	}
	
	public String getHelp() {
	    return "1830 Start Round help text";
	}

}
