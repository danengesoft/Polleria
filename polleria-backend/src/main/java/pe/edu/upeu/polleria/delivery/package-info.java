/** Delivery utiliza solo el contrato público de Pedidos y excepciones compartidas. */
@org.springframework.modulith.ApplicationModule(allowedDependencies={"pedidos::pedido-delivery", "exception"})
package pe.edu.upeu.polleria.delivery;
