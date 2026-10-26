/**
 * Inventario independiente: no consume contratos de los otros módulos
 * comerciales.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = { "exception" })
package pe.edu.upeu.polleria.abastecimiento;
