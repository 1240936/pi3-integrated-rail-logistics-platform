SELECT 
    rl.ID AS RailLineID,
    rl.STARTENDPOINT || ' - ' || rl.ENDENDPOINT AS Connection,
    SUM((ls.length) / 1000) AS TotalLength_km,
    lm.maxSpeed AS LocomotiveSpeed_kmh,

    FLOOR((SUM(ls.length) / 1000) / lm.maxSpeed) AS Hours,
    FLOOR(MOD(((SUM(ls.length) / 1000) / lm.maxSpeed) * 60, 60)) AS Minutes,
    ROUND(MOD(((SUM(ls.length) / 1000) / lm.maxSpeed) * 3600, 60)) AS Seconds

FROM LineSegment ls
JOIN RailLine rl ON ls.RailLineID = rl.ID
JOIN Locomotive l ON l.ID = 1903  
JOIN LocomotiveModel lm ON l.MODELNAME = lm.name
WHERE rl.STARTENDPOINT = 'Barcelos' AND rl.ENDENDPOINT = 'Viana do Castelo'  
GROUP BY rl.ID, rl.STARTENDPOINT, rl.ENDENDPOINT, lm.maxSpeed;