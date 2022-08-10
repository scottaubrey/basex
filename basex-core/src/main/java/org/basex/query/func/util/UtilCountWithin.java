package org.basex.query.func.util;

import static org.basex.query.func.Function.*;

import org.basex.query.*;
import org.basex.query.CompileContext.*;
import org.basex.query.expr.*;
import org.basex.query.func.*;
import org.basex.query.iter.*;
import org.basex.query.util.*;
import org.basex.query.util.list.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.util.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-22, BSD License
 * @author Christian Gruen
 */
public final class UtilCountWithin extends StandardFunc {
  @Override
  public Bln item(final QueryContext qc, final InputInfo ii) throws QueryException {
    final long[] minMax = minMax(qc);
    final long min = minMax[0], max = minMax[1];

    // iterative access: if the iterator size is unknown, iterate through results
    final Iter input = exprs[0].iter(qc);
    long size = input.size();
    if(size == -1) {
      if(max == Long.MAX_VALUE) {
        // >= min: skip if minimum is reached
        do ++size; while(size < min && qc.next(input) != null);
      } else {
        // min - max: skip if maximum is reached
        do ++size; while(size <= max && qc.next(input) != null);
      }
    }
    return Bln.get(size >= min && size <= max);
  }

  @Override
  protected void simplifyArgs(final CompileContext cc) throws QueryException {
    exprs[0] = exprs[0].simplifyFor(Simplify.COUNT, cc);
  }

  @Override
  protected Expr opt(final CompileContext cc) throws QueryException {
    final Expr input = exprs[0];

    // return statically known size (ignore non-deterministic expressions, e.g. count(error()))
    final long[] minMax = minMaxValues(cc.qc);
    if(minMax != null) {
      final long min = minMax[0], max = minMax[1];
      if(min > max) return Bln.FALSE;
      if(min <= 0 && max == Long.MAX_VALUE) return Bln.TRUE;

      // evaluate static result size
      final long size = input.size();
      if(size >= 0 && !input.has(Flag.NDT)) return Bln.get(size >= min && size <= max);

      if(max == 0) return cc.function(EMPTY, info, input);
      if(min == 1 && max == Long.MAX_VALUE) return cc.function(EXISTS, info, input);

      if(input.seqType().zeroOrOne()) {
        if(min < 1 && max <= 1) return Bln.TRUE;
        if(min >= 2) return Bln.FALSE;
      }
    }

    final Expr embedded = embed(cc, true);
    if(embedded != null) return embedded;

    return this;
  }

  @Override
  public Expr mergeEbv(final Expr expr, final boolean or, final CompileContext cc)
      throws QueryException {

    final long[] mm = minMaxValues(cc.qc);
    long[] cmm = null;
    if(mm != null) {
      if(_UTIL_COUNT_WITHIN.is(expr)) cmm = ((UtilCountWithin) expr).minMaxValues(cc.qc);
      else if(EXISTS.is(expr))  cmm = new long[] { 1, Long.MAX_VALUE };
      else if(EMPTY.is(expr))   cmm = new long[] { 0, 0 };
    }
    if(cmm != null && exprs[0].equals(expr.arg(0)) && (!or || mm[1] >= cmm[0] && mm[0] <= cmm[1])) {
      final long mn = or ? Math.min(mm[0], cmm[0]) : Math.max(mm[0], cmm[0]);
      final long mx = or ? Math.max(mm[1], cmm[1]) : Math.min(mm[1], cmm[1]);
      final ExprList args = new ExprList(3).add(exprs[0]).add(Int.get(mn));
      if(mx < Long.MAX_VALUE) args.add(Int.get(mx));
      return cc.function(_UTIL_COUNT_WITHIN, info, args.finish());
    }
    return null;
  }

  /**
   * Returns the minimum and maximum values.
   * @param qc query context
   * @return min/max values or {@code null}
   * @throws QueryException query exception
   */
  private long[] minMaxValues(final QueryContext qc) throws QueryException {
    return exprs[1] instanceof Value && (exprs.length == 2 || exprs[2] instanceof Value) ?
      minMax(qc) : null;
  }

  /**
   * Returns the minimum and maximum values.
   * @param qc query context
   * @return min/max values
   * @throws QueryException query exception
   */
  private long[] minMax(final QueryContext qc) throws QueryException {
    final long min = toLong(exprs[1], qc), max = exprs.length == 2 ? Long.MAX_VALUE :
      exprs[1] == exprs[2] ? min : toLong(exprs[2], qc);
    return new long[] { min, max };
  }
}
