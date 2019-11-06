package de.novatec.showcase.supplier.ejb.session;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.novatec.showcase.manufacture.dto.ComponentDemand;
import de.novatec.showcase.manufacture.dto.ComponentDemands;
import de.novatec.showcase.supplier.ejb.entity.PurchaseOrder;
import de.novatec.showcase.supplier.ejb.entity.PurchaseOrderLine;
import de.novatec.showcase.supplier.ejb.entity.PurchaseOrderLinePK;
import de.novatec.showcase.supplier.ejb.entity.SComponent;
import de.novatec.showcase.supplier.ejb.entity.Supplier;
import de.novatec.showcase.supplier.ejb.entity.SupplierComponent;
import de.novatec.showcase.supplier.ejb.entity.SupplierComponentPK;
import de.novatec.showcase.supplier.util.RandomTypes;;

@Stateless
@TransactionAttribute(javax.ejb.TransactionAttributeType.REQUIRED)
public class SupplierSession implements SupplierSessionLocal {

	private static Logger log = LoggerFactory.getLogger(SupplierSession.class);

	@PersistenceContext
	private EntityManager em;

	private Supplier findSupplier(ComponentDemand demand) throws NoValidSupplierFoundException {
		TypedQuery<SupplierComponent> query = em.createNamedQuery(SupplierComponent.FIND_SUPPCOMPONENT_BY_COMPONENT_ID,
				SupplierComponent.class);
		query.setParameter("id", demand.getComponentId());
		List<SupplierComponent> supplierComponents = query.getResultList();
		
		if(supplierComponents.isEmpty())
		{
			throw new NoValidSupplierFoundException("No valid Supplier found for " + demand + "!");
		}
		else
		{
			if (supplierComponents.size() == 1) {
				return em.find(Supplier.class, supplierComponents.get(0).getPk().getSupplierId());
			}

			SupplierComponent supplierComponent = null;
			for (SupplierComponent currentSupplierComponent : supplierComponents) {
				if (demand.getQuantity() > currentSupplierComponent.getQuantityForDiscount()) {
					if (supplierComponent == null
							|| currentSupplierComponent.getDiscount().compareTo(supplierComponent.getDiscount()) > 0) {
						supplierComponent = currentSupplierComponent;
					}
				}
			}
			return em.find(Supplier.class, supplierComponent.getPk().getSupplierId());
		}
	}

	@SuppressWarnings("unused")
	private SComponent findComponent(Integer id) {
		return em.find(SComponent.class, id);
	}

	@Override
	public PurchaseOrder getPurchaseOrder(Integer poNumber) {
		return em.find(PurchaseOrder.class, poNumber);
	}
	

	@Override
	public PurchaseOrderLine getPurchaseOrderLine(PurchaseOrderLinePK purchaseOrderLinePk) {
		return em.find(PurchaseOrderLine.class, purchaseOrderLinePk);
	}

	public Collection<SComponent> getAllSComponent() {
		return em.createNamedQuery(SComponent.ALL_SCOMPONENT, SComponent.class).getResultList();
	}

	@Override
	public Collection<PurchaseOrder> getAllPurchaseOrders() {
		return em.createNamedQuery(PurchaseOrder.FIND_ALL_PURCHASEORDER, PurchaseOrder.class).getResultList();
	}

	private PurchaseOrderLine createPurchaseOrderLine(PurchaseOrder purchaseOrder, SupplierComponent supplierComponent,
			ComponentDemand componentDemand) {
		Calendar since = Calendar.getInstance();
		since.set(2002, Calendar.JANUARY, 12);

		Calendar to = Calendar.getInstance();
		to.setTimeInMillis(System.currentTimeMillis());

		PurchaseOrderLine popurchaseOrderLine = new PurchaseOrderLine(getNextPolNumber(), purchaseOrder.getPoNumber(),
				componentDemand.getLocation(), supplierComponent.getPk().getComponentId(),
				componentDemand.getQuantity(), BigDecimal.valueOf(RandomTypes.getDouble(1, 10000)),
				new java.sql.Date((RandomTypes.getDate(since, to)).getTimeInMillis()),
				RandomTypes.getString("COMMENT_", 20, 100), RandomTypes.getInt(1, 100), RandomTypes.getInt(1, 100),
				purchaseOrder);

		return popurchaseOrderLine;
	}

	private int getNextPolNumber() {
		TypedQuery<Long> query = em.createNamedQuery(PurchaseOrderLine.COUNT_PURCHASEORDERLINE, Long.class);
		int nextPolNumber = query.getSingleResult().intValue();
		return ++nextPolNumber;
	}

	@Override
	public Collection<Supplier> getAllSuppliers() {
		return em.createNamedQuery(Supplier.ALL_SUPPLIERS, Supplier.class).getResultList();
	}

	@Override
	public Supplier getSupplier(Integer supplierId) {
		return em.find(Supplier.class, supplierId);
	}

	@Override
	public Collection<SupplierComponent> getAllSupplierComponents() {
		return em.createNamedQuery(SupplierComponent.FIND_ALL_SUPPCOMPONENT, SupplierComponent.class).getResultList();
	}

	@Override
	public SupplierComponent getSupplierComponent(SupplierComponentPK supplierComponentPK) {
		return em.find(SupplierComponent.class, supplierComponentPK);
	}

	@Override
	public boolean setSupplierURLs() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean setAllSupplierURLs() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Supplier createSupplier(Supplier supplier) {
		em.persist(supplier);
		em.flush();
		return supplier;
	}

	/**
	 * Throws NoValidSupplierFoundException if for one of the ComponentDemands no valid Supplier is found
	 * TODO: refactor, so that there will be a return object which shows the processed ComponentDemands with Supplier and the
	 * not processed ComponentDemands without Supplier, so that the caller can decide what to do with the not processed
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void purchase(ComponentDemands componentDemands) throws NoValidSupplierFoundException {

		Map<Integer, PurchaseOrder> purchaseOrders = new HashMap<Integer, PurchaseOrder>();

		for (ComponentDemand componentDemand : componentDemands.getComponentDemands()) {
			Supplier supplier = findSupplier(componentDemand);
			PurchaseOrder purchaseOrder = null;
			if (!purchaseOrders.containsKey(supplier.getId())) {
				purchaseOrder = createPurchaseOrder(supplier);
				purchaseOrders.put(supplier.getId(), purchaseOrder);
				em.persist(purchaseOrder);
			}
			purchaseOrder = purchaseOrders.get(supplier.getId());
			TypedQuery<SupplierComponent> query = em
					.createNamedQuery(SupplierComponent.FIND_SUPPCOMPONENT_BY_SC_SUPP_ID, SupplierComponent.class);
			query.setParameter("id", purchaseOrder.getSupplierId());
			SupplierComponent supplierComponent = query.getSingleResult();
			PurchaseOrderLine purchaseOrderLine = createPurchaseOrderLine(purchaseOrder, supplierComponent,
					componentDemand);
			purchaseOrder.addPurchaseOrderLine(purchaseOrderLine);
			em.persist(purchaseOrderLine);
		}
	}

	private PurchaseOrder createPurchaseOrder(Supplier supplier) {
		Calendar from = Calendar.getInstance();
		from.set(2002, 0, 1);

		Calendar to = Calendar.getInstance();
		to.setTimeInMillis(System.currentTimeMillis());

		Calendar date = RandomTypes.getDate(from, to);
		date.add(Calendar.DAY_OF_MONTH, RandomTypes.getInt(1, 1000));

		PurchaseOrder purchaseOrder = new PurchaseOrder(RandomTypes.getInt(1, 100), supplier.getId(),
				new java.sql.Timestamp((RandomTypes.getDate(from, to)).getTimeInMillis()),
				new java.sql.Date((RandomTypes.getDate(from, to)).getTimeInMillis()), 0);
		return purchaseOrder;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public SupplierComponent createSupplierComponent(SupplierComponent supplierComponent) {
		em.persist(supplierComponent);
		em.flush();
		return supplierComponent;
	}

	@Override
	public void processDelivery(PurchaseOrder purchaseOrder) {
		List<ComponentDemand> componentDemands = new ArrayList<ComponentDemand>();
		for (PurchaseOrderLine purchaseOrderLine : purchaseOrder.getPurchaseOrderlines()) {
			componentDemands.add(new ComponentDemand(purchaseOrderLine.getPartNumber(),
					purchaseOrderLine.getOrderedQuantity(), purchaseOrderLine.getDeliveryLocation()));
		}
		log.info(
				"TODO: SupplierSession.processDelivery(PurchaseOrder) should implement sending of ComponentDemands to manufacturedomain purchase method");
	}
}